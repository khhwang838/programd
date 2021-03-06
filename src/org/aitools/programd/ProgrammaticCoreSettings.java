/*
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version. You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 */
package org.aitools.programd;

import java.io.FileNotFoundException;
import org.aitools.util.resource.URITools;
import org.aitools.util.resource.URLTools;
import org.aitools.util.runtime.UserError;

/**
 * Automatically generated at 2011-12-30T11:01:04.665+01:00.
 */
public class ProgrammaticCoreSettings extends CoreSettings
{
    
    /**
     * Creates a <code>ProgrammaticCoreSettings</code> with default settings values.
     * These are read from the schema when this Java source file is automatically generated.
     */
    public ProgrammaticCoreSettings()
    {
        initialize();
    }
    
    /**
     * Initializes the Settings with default values (from the schema).
     */
    @Override
    protected void initialize()
    {
        try
        {
            setAIMLNamespaceURI(URITools.createValidURI("http://alicebot.org/2001/AIML-1.0.1", false));
        }
        catch (FileNotFoundException e)
        {
            throw new UserError("Error in settings.", e);
        }
        setPredicateEmptyDefault("undefined");
        setClientNamePredicate("name");
        setBotNameProperty("name");
        setPredicateFlushPeriod(Integer.parseInt("500"));
        setPredicateManagerImplementation("org.aitools.programd.predicates.InMemoryPredicateManager");
        setDatabaseDriver("com.mysql.jdbc.Driver");
        setDatabaseURI("programd");
        setDatabaseUsername("yourusername");
        setDatabasePassword("yourpassword");
        setDatabaseMinIdle(Integer.parseInt("30"));
        setDatabaseMaxActive(Integer.parseInt("70"));
        setMergePolicy(MergePolicy.COMBINE);
        setNoteEachMerge(Boolean.parseBoolean("true"));
        setAppendMergeSeparatorString(" ");
        setResponseTimeout(Integer.parseInt("1000"));
        setInfiniteLoopInput("INFINITE LOOP");
        setPrintStackTraceOnUncaughtExceptions(Boolean.parseBoolean("true"));
        setPulseImplementation("org.aitools.programd.util.IAmAlivePulse");
        setHeartPulseRate(Integer.parseInt("5"));
        setAIMLWatcherTimer(Integer.parseInt("2000"));
        setJavascriptInterpreterClassname("org.aitools.programd.interpreter.RhinoInterpreter");
        try
        {
            setSystemInterpreterDirectory(URLTools.createValidURL("..", false));
        }
        catch (FileNotFoundException e)
        {
            throw new UserError("Error in settings.", e);
        }
        setCategoryLoadNotificationInterval(Integer.parseInt("1000"));
        setNoteEachLoadedFile(Boolean.parseBoolean("false"));
        setExitImmediatelyOnStartup(Boolean.parseBoolean("false"));
        setConnectString("connect");
        setRandomStrategy(RandomStrategy.NON_REPEATING);
        setGraphmapperImplementation("org.aitools.programd.graph.MemoryGraphmapper");
        setNodemapperImplementation("org.aitools.programd.graph.TwoOptimalMemoryNodemapper");
        setResetGraph(Boolean.parseBoolean("true"));
        setUseShell(Boolean.parseBoolean("true"));
        setXmlCatalogPath("resources/catalog.xml");
    }
}
