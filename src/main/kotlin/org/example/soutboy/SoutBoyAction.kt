package org.example.soutboy

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil

class SoutBoyAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val editor: Editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val psiFile: PsiFile = e.getData(CommonDataKeys.PSI_FILE) ?: return
        val project = e.project ?: return

        val offset = editor.caretModel.offset
        val lineStartOffset = editor.document.getLineStartOffset(editor.document.getLineNumber(offset))
        val selectedText = editor.selectionModel.selectedText
        val nearestElement = psiFile.findElementAt(offset) ?: psiFile.findElementAt(offset - 1)
        val statement = PsiTreeUtil.getParentOfType(nearestElement, PsiStatement::class.java)

        val element = psiFile.findElementAt(offset) ?: psiFile.findElementAt(offset - 1)
        val className = PsiTreeUtil.getParentOfType(element, PsiClass::class.java)?.name ?: "?"
        val selectionStart = minOf(editor.selectionModel.selectionStart, editor.selectionModel.selectionEnd)
        val lineNumber = editor.document.getLineNumber(selectionStart) + 1

        val variableNames: List<String>? = run {
            val selected = selectedText?.trim()?.takeIf { it.isNotEmpty() }
            if (selected != null) {
                if (selected.contains(",")) {
                    selected.split(",")
                        .map { it.trim().split(Regex("\\s+")).last() }
                } else {
                    val selectedElement = psiFile.findElementAt(selectionStart)
                    val localVar = PsiTreeUtil.findChildOfType(
                        PsiTreeUtil.getParentOfType(selectedElement, PsiStatement::class.java),
                        PsiLocalVariable::class.java
                    )
                    val name = localVar?.name ?: run {
                        val last = selected.split(Regex("\\s+")).last()

                        val looksLikeType = last.isNotEmpty() && last[0].isUpperCase() && !last.contains(".")
                        if (isTypeReference(selectedElement) || looksLikeType) null else last
                    } ?: return@run null
                    listOf(name)
                }
            } else {
                val single = findIdentifierNearCursor(psiFile, offset, lineStartOffset)?.text
                    ?: when (statement) {
                        is PsiIfStatement -> statement.condition?.text
                        is PsiWhileStatement -> statement.condition?.text
                        is PsiForStatement -> statement.condition?.text
                        is PsiForeachStatement -> statement.iteratedValue?.text
                        else -> statement?.children
                            ?.filterIsInstance<PsiLocalVariable>()
                            ?.firstOrNull()?.name
                            ?: statement?.text?.replace(Regex("[;\\n\\r]"), "")?.trim()
                    } ?: return
                listOf(single)
            }
        }
            ?.map { it.replace(Regex("[;\\[\\]{}\"'()\\s]"), "") }
            ?.filter { it.isNotEmpty() && it.matches(Regex("[a-zA-Z_][a-zA-Z0-9_]*")) }

        if (variableNames.isNullOrEmpty()) return
        println(selectedText)
        WriteCommandAction.runWriteCommandAction(project) {
            val document = editor.document
            val normalizedOffset = minOf(editor.selectionModel.selectionStart, editor.selectionModel.selectionEnd)
            val psiElement = psiFile.findElementAt(normalizedOffset) ?: psiFile.findElementAt(normalizedOffset - 1)
            val isParameter = PsiTreeUtil.getParentOfType(psiElement, PsiParameter::class.java) != null
            val statementInner = PsiTreeUtil.getParentOfType(psiElement, PsiStatement::class.java)

            val insertOffset = if (selectedText?.contains(",") == true || isParameter) {
                val method = PsiTreeUtil.getParentOfType(psiElement, PsiMethod::class.java)
                val lBrace = method?.body?.lBrace
                lBrace?.textOffset?.plus(1) ?: statementInner?.textRange?.endOffset ?: 0
            } else if (statementInner != null ) {
                when (statementInner) {
                    is PsiIfStatement -> {
                        val thenBlock = statementInner.thenBranch
                        if (thenBlock is PsiBlockStatement) {
                            val rBrace = thenBlock.codeBlock.rBrace
                            if (rBrace != null) {
                                document.getLineStartOffset(document.getLineNumber(rBrace.textOffset)) - 1
                            } else {
                                statementInner.textRange.endOffset
                            }
                        } else {
                            statementInner.textRange.endOffset
                        }
                    }
                    is PsiReturnStatement -> {
                        val returnLine = document.getLineNumber(statementInner.textOffset)
                        document.getLineStartOffset(returnLine) - 1
                    }
                    else -> statementInner.textRange.endOffset
                }
            } else {
                val text = document.text
                val semicolonOffset = text.indexOf(';', offset).takeIf { it >= 0 }
                    ?: document.getLineEndOffset(document.getLineNumber(offset))
                semicolonOffset + 1
            }

            val caretLine = document.getLineNumber(offset)
            val lineStartOffsetInner = document.getLineStartOffset(caretLine)
            val lineEndOffset = document.getLineEndOffset(caretLine)
            val lineText = document.getText(TextRange(lineStartOffsetInner, lineEndOffset))
            val indent = lineText.takeWhile { it == ' ' || it == '\t' }
            println(variableNames)
            val logBlock = variableNames.joinToString("\n$indent") { name ->
                """System.out.println("☢️ $lineNumber ~ $className ~ $name: " + $name);"""
            }

            document.insertString(insertOffset, "\n$indent$logBlock")
            editor.caretModel.moveToOffset(insertOffset + 1 + indent.length + logBlock.length)
        }
    }

    override fun update(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR)
        e.presentation.isEnabledAndVisible = editor != null
    }

    fun isTypeReference(element: PsiElement?): Boolean {
        val parent = element?.parent
        return parent is PsiTypeElement ||
                parent is PsiJavaCodeReferenceElement ||
                parent is PsiClassObjectAccessExpression ||
                (parent is PsiReferenceExpression && parent.parent is PsiMethodCallExpression && element == parent.firstChild)
    }

    fun findIdentifierNearCursor(psiFile: PsiFile, offset: Int, lineStartOffset: Int): PsiIdentifier? {
        var current = offset
        while (current >= lineStartOffset) {
            val element = psiFile.findElementAt(current)
            if (element is PsiIdentifier) {
                val parent = element.parent
                if (parent is PsiLocalVariable && parent.nameIdentifier == element) {
                    return element
                }
                if (parent is PsiTypeElement || parent is PsiJavaCodeReferenceElement) {
                    current--
                    continue
                }
            }
            current--
        }
        return null
    }
}