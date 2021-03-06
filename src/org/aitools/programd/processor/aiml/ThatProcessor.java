/*
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version. You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 */

package org.aitools.programd.processor.aiml;

import org.aitools.programd.Core;
import org.aitools.programd.parser.TemplateParser;
import org.jdom.Element;

/**
 * Processes a <a href="http://aitools.org/aiml/TR/2001/WD-aiml/#section-template-side-that">template-side
 * <code>that</code> </a> element.
 * 
 * @author Jon Baer
 * @author Thomas Ringate, Pedro Colla
 * @author <a href="mailto:noel@aitools.org">Noel Bush</a>
 */
public class ThatProcessor extends IndexedPredicateProcessor {

  /** The label (as required by the registration scheme). */
  public static final String label = "that";

  /**
   * Creates a new ThatProcessor using the given Core.
   * 
   * @param core the Core object to use
   */
  public ThatProcessor(Core core) {
    super(core);
  }

  /**
   * Generalizes the processing of a <code>that</code> element to a job for {@link IndexedPredicateProcessor}.
   * 
   * @param element the <code>bot</code> element
   * @param parser the parser that is at work
   * @return the result of processing the element
   */
  @Override
  public String process(Element element, TemplateParser parser) {
    return super.process(element, parser, label, 2);
  }
}
