package divconq.web.md.process;

import java.util.ArrayList;
import java.util.List;

public class Configuration {
	protected boolean safeMode = true;
    protected List<Plugin> plugins = new ArrayList<>();

    /**
     * Enables HTML safe mode.
     * 
     * Default: <code>false</code>
     * 
     * @return This builder
     * @since 0.7
     */
    public Configuration enableSafeMode()
    {
        this.safeMode = true;
        return this;
    }
    
    /**
     * Sets the HTML safe mode flag.
     * 
     * Default: <code>false</code>
     * 
     * @param flag
     *            <code>true</code> to enable safe mode
     * @return This builder
     * @since 0.7
     */
    public Configuration setSafeMode(boolean flag)
    {
        this.safeMode = flag;
        return this;
    }
    
    /**
     * Sets the plugins.
     * 
     * @param plugins
     *            The plugins.
     * @return This builder.
     */
    public Configuration registerPlugins(Plugin... plugins)
    {
    	for(Plugin plugin : plugins) 
            this.plugins.add(plugin);        		
    	
        return this;
    }
    
    public Configuration() {
    }
}
