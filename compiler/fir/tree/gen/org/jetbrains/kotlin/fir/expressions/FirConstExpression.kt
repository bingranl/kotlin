/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions

import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class FirConstExpression<T> : FirExpression() {
    abstract override val source: FirSourceElement?
    abstract override val typeRef: FirTypeRef
    abstract override val annotations: List<FirAnnotationCall>
    abstract val kind: FirConstKind<T>
    abstract val value: T

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R = visitor.visitConstExpression(this, data)

    abstract override fun replaceTypeRef(newTypeRef: FirTypeRef)

    abstract fun replaceKind(newKind: FirConstKind<T>)

    abstract override fun <D> transformAnnotations(transformer: FirTransformer<D>, data: D): FirConstExpression<T>
}
