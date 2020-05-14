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

import com.biglybt.ui.swt.Utils;
import com.biglybt.ui.swt.pif.*;
import com.biglybt.core.util.Constants;
import com.biglybt.pif.Plugin;
import com.biglybt.pif.PluginConfig;
import com.biglybt.pif.PluginInterface;
import com.biglybt.pif.download.Download;
import com.biglybt.pif.ui.UIInstance;
import com.biglybt.pif.ui.UIManager;
import com.biglybt.pif.ui.UIManagerListener;
import com.biglybt.pif.ui.config.BooleanParameter;
import com.biglybt.pif.ui.config.StringListParameter;
import com.biglybt.pif.ui.model.BasicPluginConfigModel;
import com.biglybt.pif.utils.LocaleUtilities;


public class Plugin3D implements Plugin {
  
	public static final String	PLUGIN_LANG_RESOURCE 	= "com.aelitis.azureus.plugins.view3d.internat.Messages";
  
	private PluginInterface 	pluginInterface;
	private PluginConfig 		pluginConfig;
	private LocaleUtilities 	localeUtils;
  
	protected final static String VIEWID_ALL 			= "view3d.name";
	protected final static String VIEWID_SUBTAB 		= "view3d.subtab.name";
	protected final static String VIEWID_MOST_ACTIVE 	= "view3d.most.active.name";
	
	private UISWTInstance swtInstance = null;
	
	public static final String LAUNCH_ON_START = "Launch on start";
	public static final String ROTATION_SPEED = "Rotation Speed";
	
	private boolean bLaunchOnStart;
	
	private final HashMap<Integer, Object> Params = new HashMap<>();
	
  
  @Override
  public void
  initialize(
    PluginInterface   _pi )
  {
	  if ( Constants.isJava10OrHigher ){

		  	// we have to use this as Java 11 has changes that cause our sys_paths hack below to cause massive
		  	// class loading issues...
		  
		  System.setProperty( "org.lwjgl.librarypath", _pi.getPluginDirectoryName());
		  
	  }else{
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
			  // e.printStackTrace(); should probably have been using this all along... works with JDK10+ anyway
			  System.setProperty( "org.lwjgl.librarypath", _pi.getPluginDirectoryName());
		  }
	  }
    
    pluginInterface  = _pi;
    pluginConfig = pluginInterface.getPluginconfig();
	
    localeUtils = pluginInterface.getUtilities().getLocaleUtilities();
	
	localeUtils.integrateLocalisedMessageBundle( PLUGIN_LANG_RESOURCE );
    
	UIManager	ui_manager = pluginInterface.getUIManager();
	
	BasicPluginConfigModel config_model = ui_manager.createBasicPluginConfigModel( "plugins", "plugins.view3d");
	
	final BooleanParameter bP1 = config_model.addBooleanParameter2(LAUNCH_ON_START, "view3d.options.launch", false);
	
	config_model.createGroup( "view3d.options.launch.title", bP1);
	
	String[] values = {"0","1","2","3"};
	String[] labels = {	localeUtils.getLocalisedMessageText("view3d.options.display.rotation.none"),
						localeUtils.getLocalisedMessageText("view3d.options.display.rotation.slow"),
						localeUtils.getLocalisedMessageText("view3d.options.display.rotation.normal"),
						localeUtils.getLocalisedMessageText("view3d.options.display.rotation.fast")
					};
	final StringListParameter slPDisplayRotationSpeed = config_model.addStringListParameter2(ROTATION_SPEED, "view3d.options.display.rotation", values, labels, "1");
	
	final BooleanParameter b_accum = config_model.addBooleanParameter2( "view3d.options.use_accum", "view3d.options.use_accum", true );

	
	config_model.createGroup( "view3d.options.display.title",
			slPDisplayRotationSpeed, b_accum);
    
    bLaunchOnStart = pluginConfig.getPluginBooleanParameter(LAUNCH_ON_START);
    
    Params.put(0, bLaunchOnStart?"1":"0");
    Params.put(1, slPDisplayRotationSpeed.getValue());
    Params.put(2, b_accum.getValue());
    
    b_accum.addListener(param -> Params.put(2, b_accum.getValue()));
    
	
	pluginInterface.getUIManager().addUIListener(
			new UIManagerListener()
			{
				@Override
				public void
				UIAttached(
					UIInstance		instance )
				{
						if (!(instance instanceof UISWTInstance)) {
							return;
						}

						swtInstance = (UISWTInstance) instance;

						swtInstance.registerView(UISWTInstance.VIEW_MAIN,
								swtInstance.createViewBuilder(VIEWID_ALL, ViewListener.class));
						swtInstance.registerView(UISWTInstance.VIEW_MAIN,
								swtInstance.createViewBuilder(VIEWID_MOST_ACTIVE,
										ViewListener.class));

						swtInstance.registerView(Download.class,
								swtInstance.createViewBuilder(VIEWID_SUBTAB,
										ViewListener.class));

						if (bLaunchOnStart) {
							swtInstance.openView(UISWTInstance.VIEW_MAIN, VIEWID_ALL, null,
									false);
						}
					}
				
				@Override
				public void
				UIDetached(
					UIInstance		instance )
				{
				}
			});
    
  }
  
  public String
  getMessageText(
		 String		key )
  {
	  return( localeUtils.getLocalisedMessageText( key ));
  }

	public HashMap getParams() {
		return Params;
	}

	public static class ViewListener implements UISWTViewEventListener {
		private	Composite	composite;
		private	Panel3D		panel;
		private Download	ds;
		private Plugin3D plugin;
		private PluginInterface pluginInterface;

		@Override
		public boolean eventOccurred(UISWTViewEvent event) {
			UISWTView view = event.getView();
			
			switch (event.getType()) {
			
				case UISWTViewEvent.TYPE_CREATE:
					pluginInterface = event.getView().getPluginInterface();
					plugin = (Plugin3D) pluginInterface.getPlugin();
					break;

				case UISWTViewEvent.TYPE_INITIALIZE:{

					composite = (Composite)event.getData();
							
					break;
				}
				case UISWTViewEvent.TYPE_REFRESH:{

					if ( panel != null ) {
					
						panel.refresh();
					}
					
					break;
				}
				case UISWTViewEvent.TYPE_SHOWN:{

					if ( panel != null ) {
						
						panel.delete();
						
						panel = null;
					}
					
					Composite comp = composite;
				
					Utils.disposeComposite( comp, false);
					
					panel = new Panel3D( plugin, pluginInterface, view.getViewID(), plugin.getParams() );
					
					panel.initialize( comp );
				
					if ( ds != null ) {
						
						panel.setDataSource( ds );
					}
					
					comp.layout( true, true );
					
					break;
				}
				case UISWTViewEvent.TYPE_DATASOURCE_CHANGED:{
					Object ds = event.getData();
					
					Download download = null;
					
					if ( ds instanceof Download ){
						download = (Download)ds;
					}else if ( ds instanceof Object[]){
						Object[] o = (Object[])ds;
						
						if ( o[0] instanceof Download ) {
							download = (Download)o[0];
						}
					}
										
					this.ds = download;
					
					if (panel != null ){
					
						panel.setDataSource( download );
					}
					break;
				}
				case UISWTViewEvent.TYPE_HIDDEN:{

						if ( panel != null){
						
							ds = panel.getDataSouce();
							
							panel.delete();
							
							panel = null;
						}
						
						Utils.disposeComposite(composite,false);
					
					break;
				}

				case UISWTViewEvent.TYPE_DESTROY:{
					if ( panel != null){
					
						panel.delete();
					}
					
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
