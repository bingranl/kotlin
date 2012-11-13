/*
 * Copyright 2010-2012 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.jet.test.generator;

import com.google.common.collect.Lists;
import com.intellij.openapi.util.io.FileUtil;
import junit.framework.TestCase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.utils.Printer;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * @author abreslav
 */
public class TestGenerator {

    private static final List<String> JUNIT3_IMPORTS = Arrays.asList(
            "junit.framework.Assert",
            "junit.framework.Test",
            "junit.framework.TestSuite"
    );

    private final String baseDir;
    private final String suiteClassPackage;
    private final String suiteClassName;
    private final String baseTestClassPackage;
    private final String baseTestClassName;
    private final Collection<TestClassModel> testClassModels;
    private final String generatorName;

    public TestGenerator(
            @NotNull String baseDir,
            @NotNull String suiteClassPackage,
            @NotNull String suiteClassName,
            @NotNull Class<? extends TestCase> baseTestClass,
            @NotNull Collection<? extends TestClassModel> testClassModels,
            @NotNull Class<?> generatorClass
    ) {
        this.baseDir = baseDir;
        this.suiteClassPackage = suiteClassPackage;
        this.suiteClassName = suiteClassName;
        this.baseTestClassPackage = baseTestClass.getPackage().getName();
        this.baseTestClassName = baseTestClass.getSimpleName();
        this.testClassModels = Lists.newArrayList(testClassModels);
        this.generatorName = generatorClass.getCanonicalName();
    }

    public void generateAndSave() throws IOException {
        StringBuilder out = new StringBuilder();
        Printer p = new Printer(out);

        p.print(FileUtil.loadFile(new File("injector-generator/copyright.txt")));
        p.println("package ", suiteClassPackage, ";");
        p.println();
        for (String importedClassName : JUNIT3_IMPORTS) {
            p.println("import ", importedClassName, ";");
        }
        p.println();

        p.println("import java.io.File;");
        p.println("import org.jetbrains.jet.JetTestUtils;");
        p.println("import org.jetbrains.jet.test.InnerTestClasses;");
        p.println("import org.jetbrains.jet.test.TestMetadata;");
        p.println();

        p.println("import ", baseTestClassPackage, ".", baseTestClassName, ";");
        p.println();

        p.println("/** This class is generated by {@link ", generatorName, "}. DO NOT MODIFY MANUALLY */");
        if (testClassModels.size() == 1) {
            TestClassModel theOnlyTestClass = testClassModels.iterator().next();
            generateTestClass(p, new DelegatingTestClassModel(theOnlyTestClass) {
                @Override
                public String getName() {
                    return suiteClassName;
                }
            }, false);
        }
        else {
            generateTestClass(p, new TestClassModel() {
                @NotNull
                @Override
                public Collection<TestClassModel> getInnerTestClasses() {
                    return testClassModels;
                }

                @NotNull
                @Override
                public Collection<TestMethodModel> getTestMethods() {
                    return Collections.emptyList();
                }

                @Override
                public boolean isEmpty() {
                    return false;
                }

                @Override
                public String getName() {
                    return suiteClassName;
                }

                @Override
                public String getDataString() {
                    return null;
                }
            }, false);
        }

        String testSourceFilePath = baseDir + "/" + suiteClassPackage.replace(".", "/") + "/" + suiteClassName + ".java";
        File testSourceFile = new File(testSourceFilePath);
        FileUtil.writeToFile(testSourceFile, out.toString());
        System.out.println("Output written to file:\n" + testSourceFile.getAbsolutePath());
    }

    private void generateTestClass(Printer p, TestClassModel testClassModel, boolean isStatic) {
        String staticModifier = isStatic ? "static " : "";
        generateMetadata(p, testClassModel);

        generateInnerClassesAnnotation(p, testClassModel);

        p.println("public " + staticModifier + "class ", testClassModel.getName(), " extends ", baseTestClassName, " {");
        p.pushIndent();

        Collection<TestMethodModel> testMethods = testClassModel.getTestMethods();

        for (TestMethodModel testMethodModel : testMethods) {
            generateTestMethod(p, testMethodModel);
            p.println();
        }

        Collection<TestClassModel> innerTestClasses = testClassModel.getInnerTestClasses();
        for (TestClassModel innerTestClass : innerTestClasses) {
            if (innerTestClass.isEmpty()) {
                continue;
            }
            generateTestClass(p, innerTestClass, true);
            p.println();
        }

        if (!innerTestClasses.isEmpty()) {
            generateSuiteMethod(p, testClassModel, isStatic);
        }

        p.popIndent();
        p.println("}");
    }

    private static void generateSuiteMethod(Printer p, TestClassModel testClassModel, boolean innerClass) {
        String name = innerClass ? "innerSuite" : "suite";
        p.println("public static Test " + name + "() {");
        p.pushIndent();

        p.println("TestSuite suite = new TestSuite(\"", testClassModel.getName(), "\");");
        if (!testClassModel.getTestMethods().isEmpty()) {
            p.println("suite.addTestSuite(", testClassModel.getName(), ".class);");
        }
        for (TestClassModel innerTestClass : testClassModel.getInnerTestClasses()) {
            if (innerTestClass.isEmpty()) {
                continue;
            }
            if (innerTestClass.getInnerTestClasses().isEmpty()) {
                p.println("suite.addTestSuite(", innerTestClass.getName(), ".class);");
            }
            else {
                p.println("suite.addTest(", innerTestClass.getName(), ".innerSuite());");
            }
        }
        p.println("return suite;");

        p.popIndent();
        p.println("}");
    }

    private void generateTestMethod(Printer p, TestMethodModel testMethodModel) {
        generateMetadata(p, testMethodModel);
        p.println("public void ", testMethodModel.getName(), "() throws Exception {");
        p.pushIndent();

        testMethodModel.generateBody(p, generatorName);

        p.popIndent();
        p.println("}");
    }

    private static void generateMetadata(Printer p, TestEntityModel testDataSource) {
        String dataString = testDataSource.getDataString();
        if (dataString != null) {
            p.println("@TestMetadata(\"", dataString, "\")");
        }
    }

    private static void generateInnerClassesAnnotation(Printer p, TestClassModel testClassModel) {
        Collection<TestClassModel> innerTestClasses = testClassModel.getInnerTestClasses();
        if (innerTestClasses.isEmpty()) return;
        p.print("@InnerTestClasses({");
        for (Iterator<TestClassModel> iterator = innerTestClasses.iterator(); iterator.hasNext(); ) {
            TestClassModel innerTestClass = iterator.next();
            if (!innerTestClass.isEmpty()) {
                p.printWithNoIndent(testClassModel.getName(), ".", innerTestClass.getName(), ".class");
                if (iterator.hasNext()) {
                    p.printWithNoIndent(", ");
                }
            }
        }
        p.printlnWithNoIndent("})");
    }
}
