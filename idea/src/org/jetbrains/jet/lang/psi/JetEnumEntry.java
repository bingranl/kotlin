package org.jetbrains.jet.lang.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;

/**
 * @author max
 */
public class JetEnumEntry extends JetClass {
    public JetEnumEntry(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public void accept(@NotNull JetVisitor visitor) {
        visitor.visitEnumEntry(this);
    }
}
