package org.example.soutboy.model

import com.intellij.psi.PsiFile

data class LogContext (
    val psiFile: PsiFile,
    val variableNames: List<String>,
    val indent: String,
    val className:String,
    val lineNumber: Int
)