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

        val nearestElement = psiFile.findElementAt(offset)
            ?: psiFile.findElementAt(offset - 1)

        val statement = PsiTreeUtil.getParentOfType(nearestElement, PsiStatement::class.java)

        val rawVariable = run {
            val selected = selectedText?.trim()?.takeIf { it.isNotEmpty() }
            if (selected != null) {
                val selectionStart = editor.selectionModel.selectionStart
                val selectedElement = psiFile.findElementAt(selectionStart)
                val localVar = PsiTreeUtil.findChildOfType(
                    PsiTreeUtil.getParentOfType(selectedElement, PsiStatement::class.java),
                    PsiLocalVariable::class.java
                )
                localVar?.name ?: selected
            } else {
                findIdentifierNearCursor(psiFile, offset, lineStartOffset)?.text
                    ?: when (statement) {
                        is PsiIfStatement -> statement.condition?.text
                        is PsiWhileStatement -> statement.condition?.text
                        is PsiForStatement -> statement.condition?.text
                        is PsiForeachStatement -> statement.iteratedValue?.text
                        else -> statement?.children
                            ?.filterIsInstance<PsiLocalVariable>()
                            ?.firstOrNull()?.name
                            ?: statement?.text?.replace(Regex("[;\\n\\r]"), "")?.trim()
                    }
            }
        } ?: return

        val variableName = rawVariable
            .replace(Regex("[;\\[\\]{}\"']"), "")
            .takeIf { it.isNotEmpty() }
            ?: return

        val element = psiFile.findElementAt(offset) ?: psiFile.findElementAt(offset - 1)
        val className = PsiTreeUtil.getParentOfType(element, PsiClass::class.java)?.name ?: "?"
        val lineNumber = editor.document.getLineNumber(editor.selectionModel.selectionStart) + 1
        val logStatement = """System.out.println("☢️ $lineNumber ~ $className ~ $variableName: " + $variableName);"""

        WriteCommandAction.runWriteCommandAction(project) {
            val document = editor.document

            val psiElement = psiFile.findElementAt(offset) ?: psiFile.findElementAt(offset - 1)

            val statement = PsiTreeUtil.getParentOfType(psiElement, PsiStatement::class.java)

            val insertOffset = if (statement != null) {
                when (statement) {
                    is PsiIfStatement -> {
                        val thenBlock = statement.thenBranch
                        if (thenBlock is PsiBlockStatement) {
                            val rBrace = thenBlock.codeBlock.rBrace
                            if (rBrace != null) {
                                document.getLineStartOffset(document.getLineNumber(rBrace.textOffset)) - 1
                            } else {
                                statement.textRange.endOffset
                            }
                        } else {
                            statement.textRange.endOffset
                        }
                    }
                    else -> statement.textRange.endOffset
                }
            } else {
                val text = document.text
                val semicolonOffset = text.indexOf(';', offset).takeIf { it >= 0 }
                    ?: document.getLineEndOffset(document.getLineNumber(offset))
                semicolonOffset + 1
            }

            val caretLine = document.getLineNumber(offset)
            val lineStartOffset = document.getLineStartOffset(caretLine)
            val lineEndOffset = document.getLineEndOffset(caretLine)
            val lineText = document.getText(TextRange(lineStartOffset, lineEndOffset))
            val indent = lineText.takeWhile { it == ' ' || it == '\t' }

            document.insertString(insertOffset, "\n$indent$logStatement")
            editor.caretModel.moveToOffset(insertOffset + 1 + indent.length + logStatement.length)
        }
    }

    override fun update(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR)
        e.presentation.isEnabledAndVisible = editor != null
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
            }
            current--
        }
        return null
    }
}