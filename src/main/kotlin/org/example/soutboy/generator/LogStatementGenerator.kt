package org.example.soutboy.generator

import com.intellij.psi.PsiJavaFile
import org.example.soutboy.model.LogContext

object LogStatementGenerator {
    fun generateLogStatement(logContext: LogContext): String {
        val isJavaFile = logContext.psiFile is PsiJavaFile
        return if (isJavaFile) logContext.variableNames.joinToString("\n${logContext.indent}") { name ->
            """System.out.println("☢️ ${logContext.lineNumber} ~ ${logContext.className} ~ $name: " + $name);"""
        }
        else logContext.variableNames.joinToString("\n${logContext.indent}") { name ->
            """println("☢️ ${logContext.lineNumber} ~ ${logContext.className} ~ $name: $$name")"""
        }
    }
}