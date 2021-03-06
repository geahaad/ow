package net.vtst.ow.eclipse.js.closure.editor.contentassist;

import java.util.Collections;
import java.util.List;

import net.vtst.ow.closure.compiler.compile.CompilableJSUnit;
import net.vtst.ow.closure.compiler.compile.CompilerRun;
import net.vtst.ow.eclipse.js.closure.OwJsClosurePlugin;
import net.vtst.ow.eclipse.js.closure.builder.ResourceProperties;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.wst.jsdt.ui.text.java.ContentAssistInvocationContext;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.javascript.rhino.Node;

/**
 * Wrapper around {@code ContentAssistInvocationContext}.  The context distinguishes a path and
 * a prefix. For instance:
 * <pre>
 *   foo.bar.zo
 *   ^       ^ ^
 *   |       | Invocation offset
 *   |       Prefix offset   
 *   Path offset
 * </pre>
 * 
 * @author Vincent Simonet
 */
public class ClosureContentAssistIncovationContext implements IContentAssistInvocationContext {
  
  private ContentAssistInvocationContext context;

  /**
   * The offset of the first character of the path.
   */
  private int pathOffset;
  
  /**
   * The offset of the first character of the prefix.
   */
  private int prefixOffset;
  
  /**
   * The offset at which content assist is invoked.
   */
  private int invocationOffset;

  /**
   * @param context  The {@code ContentAssistInvocationContext} to wrap.
   */
  public ClosureContentAssistIncovationContext(ContentAssistInvocationContext context) {
    this.context = context;
    computePrefixAndPathOffsets();
    computeCompilerRun();
  }
  
  // **************************************************************************
  // Access to properties of the included context
  
  /**
   * Returns the document that content assist is invoked on.
   * @return  The document, or null if unknown.
   */
  public IDocument getDocument() {
    return context.getDocument();
  }
  
  /**
   * @return  The invocation offset.
   */
  public int getInvocationOffset() {
    return context.getInvocationOffset();
  }

  /**
   * Returns the viewer that content assist is invoked in.
   * @return  The viewer, or null if unknown.
   */
  public ITextViewer getViewer() {
    return context.getViewer();
  }

  // **************************************************************************
  // Prefix and path

  /**
   * Compute the prefix already present in the document at the invocation offset.
   */
  private void computePrefixAndPathOffsets() {
    IDocument document = context.getDocument();
    invocationOffset = context.getInvocationOffset();
    try {
      prefixOffset = invocationOffset;
      while (prefixOffset > 0 && isCharForPrefix(document.getChar(prefixOffset - 1))) --prefixOffset;
      pathOffset = prefixOffset;
      while (pathOffset > 0 && isCharForPath(document.getChar(pathOffset - 1))) -- pathOffset;
    } catch (BadLocationException e) {
      assert false;
    }
  }

  /**
   * Test whether a char can be part of the prefix
   * @param c  The char to test.
   * @return  true if the char can be part of the prefix.
   */
  private boolean isCharForPrefix(char c) {
    return (
        c == '_' || 
        c >= 'a' && c <= 'z' ||
        c >= 'A' && c <= 'Z' ||
        c >= '0' && c <= '9');
  }
  
  private boolean isCharForPath(char c) {
    return (c == '.' || isCharForPrefix(c));
  }

  /**
   * Returns the length of the prefix.
   * @return  The length of the prefix.
   */
  public int getPrefixLength() {
    return (invocationOffset - prefixOffset);
  }
  
  /**
   * Returns the prefix.
   * @return  The prefix.
   */
  public String getPrefix() {
    try {
      return getDocument().get(getPrefixOffset(), getPrefixLength());
    } catch (BadLocationException e) {
      assert false;
      return null;
    }
  }
  
  /**
   * @return The qualified name of the prefix of the invocation context.
   */
  public List<String> getPrefixAsQualifiedName() {
    if (prefixOffset == pathOffset) return Collections.emptyList();
    try {
      return Lists.newArrayList(
          Splitter.on('.').split(getDocument().get(pathOffset, prefixOffset - pathOffset - 1)));
    } catch (BadLocationException e) {
      assert false;
      return null;
    }
  }
  
  /**
   * Returns the offset of the first character of the prefix in the document.
   * @return
   */
  public int getPrefixOffset() {
    return prefixOffset;
  }

  // **************************************************************************
  // Compilation

  private CompilerRun run = null;
  private Node node = null;
  
  private void computeCompilerRun() {
    IFile file = OwJsClosurePlugin.getDefault().getEditorRegistry().getFile(getDocument());
    if (file != null) {
      CompilableJSUnit unit = ResourceProperties.getJSUnitOrNullIfCoreException(file);
      if (unit != null) {
        run = unit.getLastAvailableCompilerRun();
        if (run != null) {
          run.fastCompile();
          node = run.getNode(unit, getPrefixOffset());
        }
      }
    }
  }
    
  /**
   * @return Whether a node has been found for this invocation context in the output of the compiler.
   */
  public boolean hasNode() {
    return run != null;
  }

  /**
   * @return The compiler run for this invocation context.
   */
  public CompilerRun getCompilerRun() {
    return run;
  }

  /**
   * @return The node for this invocation context.
   */
  public Node getNode() {
    return node;
  }

}
