/*
 * Copyright 2003-2005 Dave Griffith
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
package com.siyeh.ig.performance;

import com.intellij.psi.*;
import com.intellij.psi.util.InheritanceUtil;
import org.jetbrains.annotations.NotNull;

class MethodReferenceVisitor extends PsiRecursiveElementVisitor{

    private boolean m_referencesStaticallyAccessible = true;
    private PsiMethod m_method;

    MethodReferenceVisitor(PsiMethod method){
        super();
        m_method = method;
    }

    public boolean areReferencesStaticallyAccessible(){
        return m_referencesStaticallyAccessible;
    }

    public void visitElement(PsiElement element) {
        if(!m_referencesStaticallyAccessible){
            return;
        }
        super.visitElement(element);
    }

    public void visitReferenceElement(PsiJavaCodeReferenceElement reference){
        super.visitReferenceElement(reference);

        final PsiElement resolvedElement = reference.resolve();
        if(!(resolvedElement instanceof PsiClass)){
            return;
        }
        final PsiClass aClass = (PsiClass) resolvedElement;
        final PsiElement scope = aClass.getScope();
        if(!(scope instanceof PsiClass)){
            return;
        }
        if (aClass.hasModifierProperty(PsiModifier.STATIC)){
            return;
        }
        m_referencesStaticallyAccessible = false;
    }

    public void visitReferenceExpression(
            @NotNull PsiReferenceExpression expression){
        super.visitReferenceExpression(expression);
        final PsiElement qualifier = expression.getQualifierExpression();
        if (qualifier != null && !(qualifier instanceof PsiThisExpression) &&
                !(qualifier instanceof PsiSuperExpression)){
            return;
        }
        final PsiElement element = expression.resolve();
        if(element instanceof PsiMember){
            if (isMemberStaticallyAccessible((PsiMember) element)){
                return;
            }
        } else {
            return;
        }
        m_referencesStaticallyAccessible = false;
    }

    public void visitThisExpression(@NotNull PsiThisExpression expression){
        super.visitThisExpression(expression);
        m_referencesStaticallyAccessible = false;
    }

    private boolean isMethodStaticallyAccessible(PsiMethod method){
        if(m_method.equals(method)){
            return true;
        }
        if(method.hasModifierProperty(PsiModifier.STATIC)){
            return true;
        }
        if(method.isConstructor()){
            return true;
        }
        final PsiClass referenceContainingClass = m_method.getContainingClass();
        final PsiClass methodContainingClass = method.getContainingClass();
        return !InheritanceUtil.isCorrectDescendant(referenceContainingClass,
                                                  methodContainingClass, true);
    }

    private boolean isMemberStaticallyAccessible(PsiMember member){
        if(m_method.equals(member)){
            return true;
        }
        if(member.hasModifierProperty(PsiModifier.STATIC)){
            return true;
        }
        final PsiClass referenceContainingClass = m_method.getContainingClass();
        final PsiClass containingClass = member.getContainingClass();
        return !InheritanceUtil.isCorrectDescendant(referenceContainingClass,
                                                  containingClass, true);
    }
}