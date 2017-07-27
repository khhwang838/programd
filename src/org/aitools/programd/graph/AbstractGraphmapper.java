/*
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version. You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 */

package org.aitools.programd.graph;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.aitools.programd.Bot;
import org.aitools.programd.Core;
import org.aitools.programd.CoreSettings;
import org.aitools.programd.parser.AIMLReader;
import org.aitools.programd.processor.aiml.RandomProcessor;
import org.aitools.util.Text;
import org.aitools.util.resource.Filesystem;
import org.aitools.util.resource.URLTools;
import org.aitools.util.runtime.Errors;
import org.aitools.util.runtime.UserError;
import org.aitools.util.xml.SAX;
import org.apache.log4j.Logger;
import org.jdom.Content;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

/**
 * @author <a href="mailto:noel@aitools.org">Noel Bush</a>
 */
abstract public class AbstractGraphmapper implements Graphmapper {

  // Instance variables.

  /** The Core with which this Graphmapper is associated. */
  protected Core _core;

  /** The logger. */
  protected Logger _logger = Logger.getLogger("programd");

  /** The match logger. */
  protected Logger _matchLogger = Logger.getLogger("programd.matching");

  /** The merge policy. */
  protected CoreSettings.MergePolicy _mergePolicy;

  /** A formatter used for outputting XML. */
  private Format _xmlFormat = Format.getPrettyFormat();

  /** Whether to note each file loaded. */
  protected boolean _noteEachLoad;

  /** The separator string to use with the "append" merge policy. */
  protected String _mergeAppendSeparator;

  /** Whether to note each merge. */
  protected boolean _noteEachMerge;

  /** Whether to use the AIML Watcher. */
  protected boolean _useAIMLWatcher;

  /** The AIML namespace URI in use. */
  protected String _aimlNamespaceURI;

  /** How frequently to provide a category load count. */
  protected int _categoryLoadNotifyInterval;

  /** The total number of categories read. */
  protected int _totalCategories = 0;

  /** The total number of path-identical categories that have been encountered. */
  protected int _duplicateCategories = 0;

  /** The response timeout. */
  protected int _responseTimeout;

  // Constants

  /** A that marker. */
  public static final String THAT = "<that>";

  /** A topic marker. */
  public static final String TOPIC = "<topic>";

  /** A bot marker. */
  public static final String BOT = "<bot>";

  /** A template marker. */
  public static final String TEMPLATE = "<template>";

  /** A filename marker. */
  public static final String FILENAME = "<filename>";

  /** The <code>*</code> wildcard. */
  public static final String ASTERISK = "*";

  /** The <code>_</code> wildcard. */
  public static final String UNDERSCORE = "_";

  /**
   * Creates a new AbstractGraphmapper, reading settings from the given Core.
   * 
   * @param core the CoreSettings object from which to read settings
   */
  protected AbstractGraphmapper(Core core) {
    this._core = core;

    CoreSettings settings = this._core.getSettings();
    this._noteEachLoad = settings.noteEachLoadedFile();
    this._mergePolicy = settings.getMergePolicy();
    this._mergeAppendSeparator = settings.getAppendMergeSeparatorString();
    this._noteEachMerge = settings.noteEachMerge();
    this._useAIMLWatcher = settings.useAIMLWatcher();
    this._responseTimeout = settings.getResponseTimeout();
    this._categoryLoadNotifyInterval = settings.getCategoryLoadNotificationInterval();
    this._aimlNamespaceURI = settings.getAIMLNamespaceURI().toString();
  }

  protected abstract void add(String pattern, String that, String topic, String template, Bot bot, URL source);

  /**
   * @see org.aitools.programd.graph.Graphmapper#addCategory(java.lang.String, java.lang.String, java.lang.String,
   *      java.lang.String, org.aitools.programd.Bot, java.net.URL)
   */
  @Override
  @SuppressWarnings("boxing")
  public void addCategory(String pattern, String that, String topic, String template, Bot bot, URL source) {
    // Make sure the path components are right.
    String _pattern = pattern == null ? ASTERISK : pattern;
    String _that = that == null ? ASTERISK : that;
    String _topic = topic == null ? ASTERISK : topic;

    // Report on loaded categories.
    if (this._totalCategories % this._categoryLoadNotifyInterval == 0 && this._totalCategories > 0) {
      this._logger.info(String.format("%,d categories loaded so far.", this._totalCategories));
    }
    this.add(_pattern, _that, _topic, template, bot, source);
  }

  /**
   * Adds the given botid to the &lt;botid&gt; node for all branches associated with the given URL. This should only be
   * called using a URL that <i>has</i> previously been loaded for <i>another</i> bot.
   * 
   * @param path
   * @param botid
   * @throws IllegalArgumentException if the given path has not already been loaded, or if it has been loaded for the
   *         same botid
   */
  abstract protected void addForBot(URL path, String botid);

  /**
   * Appends the contents of one template to another.
   * 
   * @param existingTemplate the template to which to append
   * @param newTemplate the template whose content should be appended
   * @return the combined result
   */
  @SuppressWarnings("unchecked")
  protected String appendTemplate(String existingTemplate, String newTemplate) {
    Document existingDoc;
    Element existingRoot;

    Document newDoc;
    List<Content> newContent;

    try {
      existingDoc = new SAXBuilder().build(new StringReader(existingTemplate));
      existingRoot = existingDoc.getRootElement();

      newDoc = new SAXBuilder().build(new StringReader(newTemplate));
      newContent = newDoc.getRootElement().getContent();
    }
    catch (JDOMException e) {
      this._logger.error("JDOM exception when performing merge append.", e);
      return existingTemplate;
    }
    catch (IOException e) {
      this._logger.error("IO exception when performing merge append.", e);
      return existingTemplate;
    }

    // Append whatever text is configured to be inserted between the templates.
    if (this._mergeAppendSeparator != null) {
      existingRoot.addContent(this._mergeAppendSeparator);
    }
    existingRoot.addContent(newContent);
    return new XMLOutputter(this._xmlFormat).outputString(existingDoc);
  }

  /**
   * Creates an association between the given botid and the given filename.
   * 
   * @param botid
   * @param filename
   */
  abstract protected void associateBotIDWithFilename(String botid, URL filename);

  /**
   * Combines two template content strings into a single template, using a random element so that either original
   * template content string has an equal chance of being processed. The order in which the templates are supplied is
   * important: the first one (<code>existingTemplate</code>) is processed as though it has already been stored in the
   * Graphmaster, and hence might itself be the result of a previous <code>combine()</code> operation. If this is the
   * case, the in-memory representation of the template will have a special attribute indicating this fact, which will
   * be used to &quot;balance&quot; the combine operation.
   * 
   * @param existingTemplate the template with which the new template should be combined
   * @param newTemplate the template which should be combined with the existing template
   * @return the combined result
   */
  @SuppressWarnings("unchecked")
  protected String combineTemplates(String existingTemplate, String newTemplate) {
    Document existingDoc;
    Element existingRoot;
    List<Content> existingContent;

    Document newDoc;
    List<Content> newContent = new ArrayList<Content>();

    try {
      existingDoc = new SAXBuilder().build(new StringReader(existingTemplate));
      existingRoot = existingDoc.getRootElement();
      existingContent = existingRoot.getContent();

      newDoc = new SAXBuilder().build(new StringReader(newTemplate));
      for (Content newContentItem : (List<Content>) newDoc.getRootElement().getContent()) {
        newContent.add((Content) newContentItem.clone());
      }
    }
    catch (JDOMException e) {
      this._logger.error("JDOM exception when performing merge combine.", e);
      return existingTemplate;
    }
    catch (IOException e) {
      this._logger.error("IO exception when performing merge combine.", e);
      return existingTemplate;
    }

    /*
     * If the existing template has a random element as its root, we need to check whether this was the result of a
     * previous combine.
     */
    Content firstNode = existingContent.get(0);
    if (firstNode instanceof Element) {
      Element firstElement = (Element) firstNode;
      if (firstElement.getName().equals(RandomProcessor.label) && firstElement.getAttribute("synthetic") != null) {
        Element newListItem = new Element(RandomProcessor.LI, this._aimlNamespaceURI);
        newListItem.addContent(newContent);
        firstElement.addContent(newListItem);
      }
      return new XMLOutputter(this._xmlFormat).outputString(existingDoc);
    }
    Element listItemForExisting = new Element(RandomProcessor.LI, this._aimlNamespaceURI);
    existingRoot.removeContent();
    listItemForExisting.addContent(existingContent);

    Element listItemForNew = new Element(RandomProcessor.LI, this._aimlNamespaceURI);
    listItemForNew.addContent(newContent);

    Element newRandom = new Element(RandomProcessor.label, this._aimlNamespaceURI);
    newRandom.setAttribute("synthetic", "yes");
    newRandom.addContent(listItemForExisting);
    newRandom.addContent(listItemForNew);

    existingRoot.addContent(newRandom);

    return new XMLOutputter(this._xmlFormat).outputString(existingDoc);
  }

  /**
   * Composes an input path as a list of tokens, given the components. Empty components are represented with asterisks.
   * 
   * @param input
   * @param that
   * @param topic
   * @param botid
   * @return the new path
   */
  protected static List<String> composeInputPath(String input, String that, String topic, String botid) {
    List<String> inputPath = new ArrayList<String>();

    // Input text part.
    if (input.length() > 0) {
      inputPath = Text.wordSplit(input);
    }
    else {
      inputPath = new ArrayList<String>();
      inputPath.add(ASTERISK);
    }

    // <that> marker.
    inputPath.add(THAT);

    // Input <that> part.
    if (that.length() > 0) {
      inputPath.addAll(Text.wordSplit(that));
    }
    else {
      inputPath.add(ASTERISK);
    }

    // <topic> marker.
    inputPath.add(TOPIC);

    // Input <topic> part.
    if (topic.length() > 0) {
      inputPath.addAll(Text.wordSplit(topic));
    }
    else {
      inputPath.add(ASTERISK);
    }

    // <botid> marker.
    inputPath.add(BOT);

    // Input [directed to] botid.
    inputPath.add(botid);

    return inputPath;
  }

  protected void doLoad(URL path, String botid) {

    this.beforeLoad(path, botid);
    AIMLReader handler = new AIMLReader(this, path, this._core.getBot(botid));
    XMLReader reader = SAX.getReader(handler, this._logger, this._core.getSettings().getXmlCatalogPath());
    try {
      reader.parse(path.toExternalForm());
      this.associateBotIDWithFilename(botid, path);
    }
    catch (IOException e) {
      this._logger.warn(String.format("Error reading \"%s\": %s", URLTools.unescape(path), Errors.describe(e)), e);
    }
    catch (SAXException e) {
      this._logger.warn(String.format("Error reading \"%s\": %s", URLTools.unescape(path), Errors.describe(e)));
    }
    this.afterLoad(path, botid);
  }
  

  /**
   * @see org.aitools.programd.graph.Graphmapper#beforeLoad(java.net.URL, java.lang.String)
   */
  @Override
  public void beforeLoad(URL path, String botid) {
    // Nothing done at this level, but some Graphmappers will want to do something.
  }

  /**
   * @see org.aitools.programd.graph.Graphmapper#afterLoad(java.net.URL, java.lang.String)
   */
  @Override
  public void afterLoad(URL path, String botid) {
    // Nothing done at this level, but some Graphmappers will want to do something.
  }

  /**
   * @see org.aitools.programd.graph.Graphmapper#getCategoryCount()
   */
  @Override
  public int getCategoryCount() {
    return this._totalCategories;
  }

  /**
   * @see org.aitools.programd.graph.Graphmapper#getCategoryReport()
   */
  @Override
  @SuppressWarnings("boxing")
  public String getCategoryReport() {
    return String.format("%,d total categories currently loaded.", this._totalCategories);
  }

  /**
   * @see org.aitools.programd.graph.Graphmapper#getDuplicateCategoryCount()
   */
  @Override
  public int getDuplicateCategoryCount() {
    return this._duplicateCategories;
  }

  /**
   * Indicates whether the given filename is already loaded for any bot at all.
   * 
   * @param filename
   * @return whether the given filename is already loaded
   */
  abstract protected boolean isAlreadyLoaded(URL filename);

  /**
   * Indicates whether the given filename is already loaded for the given bot.
   * 
   * @param filename
   * @param botid
   * @return whether the given filename is already loaded for the given botid
   */
  abstract protected boolean isAlreadyLoadedForBot(URL filename, String botid);

  /**
   * @see org.aitools.programd.graph.Graphmapper#load(java.net.URL, java.lang.String)
   */
  @Override
  public void load(URL path, String botid) {
    // Handle paths with wildcards that need to be expanded.
    if (path.getProtocol().equals(Filesystem.FILE)) {
      String spec = path.getFile();
      if (spec.indexOf('*') != -1) {
        List<File> files = null;

        try {
          files = Filesystem.glob(spec);
        }
        catch (FileNotFoundException e) {
          this._logger.warn(e.getMessage());
        }
        if (files != null) {
          for (File file : files) {
            this.load(URLTools.contextualize(URLTools.getParent(path), file.getAbsolutePath()), botid);
          }
        }
        return;
      }
    }

    Bot bot = this._core.getBot(botid);

    // Let the Graphmapper use a shortcut if possible.
    if (this.isAlreadyLoaded(path)) {
      if (this.isAlreadyLoadedForBot(path, botid)) {
        if (this._logger.isDebugEnabled()) {
          this._logger.debug(String.format("Reloading \"%s\" for \"%s\" (is that what you wanted?).", path, botid));
        }
        this.unload(path, bot);
        this.doLoad(path, botid);
      }
      else {
        if (this._logger.isDebugEnabled()) {
          this._logger.debug(String.format("Graphmapper has already loaded \"%s\" for some other bot.", path));
        }
        this.addForBot(path, botid);
      }
    }
    else {
      if (this._noteEachLoad) {
        this._logger.info(String.format("Loading %s....", URLTools.unescape(path)));
      }
      this.doLoad(path, botid);
      // Add it to the AIMLWatcher, if active.
      if (this._useAIMLWatcher) {
        this._core.getAIMLWatcher().addWatchFile(path);
      }
    }
  }

  abstract protected void print(PrintWriter out);

  /**
   * @see org.aitools.programd.graph.Graphmapper#print(java.lang.String)
   */
  @Override
  public void print(String path) {
    try {
      this.print(new PrintWriter(Filesystem.checkOrCreate(path, "Graphmapper output")));
    }
    catch (FileNotFoundException e) {
      throw new UserError("Cannot find file to print graph.", e);
    }

  }
}
