/*
 * Created on 30 juil. 2005
 * Created by Olivier Chalouhi
 * 
 * Copyright (C) 2004 Aelitis SARL, All rights Reserved
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details ( see the LICENSE file ).
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 * 
 * AELITIS, SARL au capital de 30,000 euros,
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 */
package com.aelitis.azureus.plugins.view3d;

import java.io.File;
import java.lang.reflect.Field;
import java.util.HashMap;

import org.eclipse.swt.widgets.Composite;
import com.biglybt.pif.Plugin;
import com.biglybt.pif.PluginConfig;
import com.biglybt.pif.PluginInterface;
import com.biglybt.pif.ui.UIInstance;
import com.biglybt.pif.ui.UIManager;
import com.biglybt.pif.ui.UIManagerListener;
import com.biglybt.pif.ui.config.BooleanParameter;
import com.biglybt.pif.ui.config.Parameter;
import com.biglybt.pif.ui.config.ParameterListener;
import com.biglybt.pif.ui.config.StringListParameter;
import com.biglybt.pif.ui.model.BasicPluginConfigModel;
import com.biglybt.pif.utils.LocaleUtilities;
import com.biglybt.ui.swt.pif.UISWTInstance;
import com.biglybt.ui.swt.pif.UISWTViewEvent;
import com.biglybt.ui.swt.pif.UISWTViewEventListener;



public class Plugin3D implements Plugin {
  
	public static final String	PLUGIN_LANG_RESOURCE 	= "com.aelitis.azureus.plugins.view3d.internat.Messages";
  
	private PluginInterface 	pluginInterface;
	private PluginConfig 		pluginConfig;
	private LocaleUtilities 	localeUtils;
  
	private final String VIEWID = "3D View";
	
	private UISWTInstance swtInstance = null;
	
	public final String LAUNCH_ON_START = "Launch on start";
	public final String ROTATION_SPEED = "Rotation Speed";
	
	private boolean bLaunchOnStart;
	
	private HashMap Params = new HashMap();
	
  
  @Override
  public void
  initialize(
    PluginInterface   _pi )
  {
    try {
      String binaryPath = _pi.getPluginDirectoryName();
      String newLibPath = binaryPath + File.pathSeparator +
      System.getProperty("java.library.path"); 
    
      System.setProperty("java.library.path", newLibPath);
      Field fieldSysPath = ClassLoader.class.getDeclaredField("sys_paths");
    
      fieldSysPath.setAccessible(true);
    
      if (fieldSysPath != null) {
        fieldSysPath.set(System.class.getClassLoader(), null);
      }
    } catch(Exception e) {
      e.printStackTrace();
    }
    
    pluginInterface  = _pi;
    pluginConfig = pluginInterface.getPluginconfig();
	
    localeUtils = pluginInterface.getUtilities().getLocaleUtilities();
	
	localeUtils.integrateLocalisedMessageBundle( PLUGIN_LANG_RESOURCE );
    
	UIManager	ui_manager = pluginInterface.getUIManager();
	
	BasicPluginConfigModel config_model = ui_manager.createBasicPluginConfigModel( "plugins", "plugins.view3d");
	
	final BooleanParameter bP1 = config_model.addBooleanParameter2(LAUNCH_ON_START, "view3d.options.launch", false);
	
	config_model.createGroup( "view3d.options.launch.title",
			new Parameter[]{ 
				bP1
			});
	
	String[] values = {"0","1","2","3"};
	String[] labels = {	localeUtils.getLocalisedMessageText("view3d.options.display.rotation.none"),
						localeUtils.getLocalisedMessageText("view3d.options.display.rotation.slow"),
						localeUtils.getLocalisedMessageText("view3d.options.display.rotation.normal"),
						localeUtils.getLocalisedMessageText("view3d.options.display.rotation.fast")
					};
	final StringListParameter slPDisplayRotationSpeed = config_model.addStringListParameter2(ROTATION_SPEED, "view3d.options.display.rotation", values, labels, "1");
	
	final BooleanParameter b_accum = config_model.addBooleanParameter2( "view3d.options.use_accum", "view3d.options.use_accum", true );

	
	config_model.createGroup( "view3d.options.display.title", 
			new Parameter[] {
				slPDisplayRotationSpeed, b_accum
	});
    
    bLaunchOnStart = pluginConfig.getPluginBooleanParameter(LAUNCH_ON_START);
    
    Params.put(new Integer(0), bLaunchOnStart?"1":"0");
    Params.put(new Integer(1), slPDisplayRotationSpeed.getValue());
    Params.put(new Integer(2), b_accum.getValue());
    
    b_accum.addListener(
    	new ParameterListener()
    	{
    		@Override
		    public void
    		parameterChanged(
    			Parameter param)
    		{
    			Params.put(new Integer(2), b_accum.getValue());
    		}
    	});
    
	pluginInterface.getUIManager().addUIListener(
			new UIManagerListener()
			{
				private ViewListener	view_listener;
				
				@Override
				public void
				UIAttached(
					UIInstance		instance )
				{
					if ( instance instanceof UISWTInstance ){
						
						swtInstance = (UISWTInstance)instance;
						view_listener = new ViewListener();

						swtInstance.addView( UISWTInstance.VIEW_MAIN, VIEWID, view_listener );
						
						if(bLaunchOnStart)
							swtInstance.openMainView(VIEWID, view_listener, null);
					}
				}
				
				@Override
				public void
				UIDetached(
					UIInstance		instance )
				{
					if ( instance instanceof UISWTInstance ) {

						((UISWTInstance) instance).removeViews(UISWTInstance.VIEW_MAIN, VIEWID);
						swtInstance = null;
					}

				}
			});
    
  }
  
	private class ViewListener implements UISWTViewEventListener {

		private Panel3D 	_3D_view;
		private boolean isCreated;
		
		@Override
		public boolean eventOccurred(UISWTViewEvent event) {
			switch (event.getType()) {
			
				case UISWTViewEvent.TYPE_CREATE:
			        if (isCreated)
			          return false;
	
			        isCreated = true;
			        break;

				case UISWTViewEvent.TYPE_INITIALIZE:{
					
					Composite	comp = (Composite)event.getData();
					
					_3D_view = new Panel3D( pluginInterface, Params );
					
					_3D_view.initialize( comp );

					
					break;
				}
				case UISWTViewEvent.TYPE_DESTROY:{
					if ( _3D_view != null ){
					
						_3D_view.delete();
					
						_3D_view	= null;
					}
					
					isCreated = false;
					
					break;
				}	
			}
			
			return true;
		}

	}
  
  public PluginInterface getPluginInterface() {
    return pluginInterface;
  }

}
