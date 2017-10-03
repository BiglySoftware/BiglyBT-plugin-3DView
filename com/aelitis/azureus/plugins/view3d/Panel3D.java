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

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

import com.biglybt.core.util.IdentityHashSet;
import com.biglybt.core.util.SystemTime;
import com.biglybt.pif.PluginInterface;
import com.biglybt.pif.download.Download;
import com.biglybt.pif.download.DownloadManager;
import com.biglybt.pif.download.DownloadListener;
import com.biglybt.pif.download.DownloadManagerListener;
import com.biglybt.ui.swt.Utils;
import com.biglybt.ui.swt.mainwindow.Colors;



public class Panel3D  {

	private static Panel3D	active_panel;
	
	private static Peers3DGraphicView
	get3DView(
		PluginInterface		plugin_interface,
		Panel3D				panel,
		Composite			c3DViewSection,
		HashMap				params )
	{
			// we can only have one active GLCanvas at a time otherwise SWT goes all weird and sidebar events start getting
			// lost (don't fully understand the cause but possibly due to the fact that the context is implicitly single
			// activity based (as in GLContext.useContext)
		
		if ( active_panel != null ) {
			if ( active_panel.getComposite().isDisposed()) {
				active_panel = null;
			}else {
				active_panel.disable3DView();
			}			
		}
		
		active_panel = panel;
		
		return( new Peers3DGraphicView(plugin_interface, c3DViewSection, params));
	}
	
  final Plugin3D			plugin;
  final PluginInterface		plugin_interface;
  
  DownloadManager 	download_manager;
  Download[] downloads;
  
  boolean	disabled;
  
  final String	view_id;
  
  Comparator comparator;
  
  Composite panel;
  Display display;

  
  Table channelTable;
  TableColumn name;
  Label lHeader;
  Font headerFont; 
  GridData data;
  TableItem[] items;
  TableItem item;
  
  Composite c3DViewSection;
  
  Peers3DGraphicView peer3DView;
  
  HashMap params = new HashMap();
  
  IdentityHashSet<Download> activeDownloads = new IdentityHashSet<>();

  public Panel3D(Plugin3D _plugin, PluginInterface _plugin_interface, String _view_id, HashMap _params) {
	  plugin				= _plugin;
	  plugin_interface 		= _plugin_interface;
	  view_id				= _view_id;
	  params 				= _params;
  }
  
  public void initialize(Composite composite) {
    
    initUI(composite);
    
    download_manager = plugin_interface.getDownloadManager();
    download_manager.addListener(new Swarm3DDMListener());
    comparator = plugin_interface.getUtilities().getFormatters().getAlphanumericComparator(true);
    
  }

  private void initUI(Composite composite) {
    display = composite.getDisplay();
    
    panel = new Composite(composite,SWT.NONE);
  
    if ( view_id.equals( Plugin3D.VIEWID_SUBTAB )) {
	    GridLayout gridLayout = new GridLayout();

	    gridLayout.numColumns = 1;
	    gridLayout.marginHeight = 0;
	    gridLayout.marginWidth = 0;   
	    panel.setLayout(gridLayout);
	    
	    data = new GridData(GridData.FILL_BOTH);
	    panel.setLayoutData(data);

	    c3DViewSection = new Composite(panel, SWT.NULL);
	    c3DViewSection.setLayout(new GridLayout());
	    data = new GridData(GridData.FILL_BOTH);
	    c3DViewSection.setLayoutData(data);
	    
    }else {
	    GridLayout gridLayout = new GridLayout();
	    gridLayout.numColumns = 2;
	    gridLayout.marginHeight = 0;
	    gridLayout.marginWidth = 0;   
	    panel.setLayout(gridLayout);
	    
	    data = new GridData(GridData.FILL_BOTH);
	    panel.setLayoutData(data);
	    
	    SashForm form = new SashForm(panel,SWT.HORIZONTAL);
	    data = new GridData(GridData.FILL_BOTH);
	    form.setLayoutData(data);
	
	    Composite cLeftSide = new Composite(form, SWT.NONE);    
	    GridLayout layout = new GridLayout();
	    layout.numColumns = 2;
	    cLeftSide.setLayout(layout);
	    
	    
	    channelTable = new Table(cLeftSide, SWT.BORDER | SWT.SINGLE);
	    data = new GridData(GridData.FILL_BOTH);
	    data.horizontalSpan = 2;
	    channelTable.setLayoutData(data);
	    channelTable.setHeaderVisible(true);
	    name = new TableColumn(channelTable,SWT.LEFT);
	    name.setText("Running Torrents");
	    
	    Composite cRightSide = new Composite(form, SWT.NONE);
	    gridLayout = new GridLayout();
	    gridLayout.marginHeight = 3;
	    gridLayout.marginWidth = 0;
	    cRightSide.setLayout(gridLayout);
	
	    // Header
	    Composite cHeader = new Composite(cRightSide, SWT.BORDER);
	    gridLayout = new GridLayout();
	    gridLayout.marginHeight = 3;
	    gridLayout.marginWidth = 0;
	    cHeader.setLayout(gridLayout);
	    data = new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_CENTER);
	    cHeader.setLayoutData(data);
	
	    Display d = cRightSide.getDisplay();
	    cHeader.setBackground(d.getSystemColor(SWT.COLOR_LIST_SELECTION));
	    cHeader.setForeground(d.getSystemColor(SWT.COLOR_LIST_SELECTION_TEXT));
	
	    lHeader = new Label(cHeader, SWT.NULL);
	    lHeader.setBackground(d.getSystemColor(SWT.COLOR_LIST_SELECTION));
	    lHeader.setForeground(d.getSystemColor(SWT.COLOR_LIST_SELECTION_TEXT));
	    FontData[] fontData = lHeader.getFont().getFontData();
	    fontData[0].setStyle(SWT.BOLD);
	    int fontHeight = (int)(fontData[0].getHeight() * 1.2);
	    fontData[0].setHeight(fontHeight);
	    headerFont = new Font(d, fontData);
	    lHeader.setFont(headerFont);
	    data = new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_CENTER);
	    lHeader.setLayoutData(data);
	    
	    // Channel Section
	    c3DViewSection = new Composite(cRightSide, SWT.NULL);
	    c3DViewSection.setLayout(new GridLayout());
	    data = new GridData(GridData.FILL_BOTH);
	    c3DViewSection.setLayoutData(data);
	    
	    form.setWeights(new int[] {20,80});
	    
	    if ( view_id.equals( Plugin3D.VIEWID_MOST_ACTIVE )) {
	    	cLeftSide.setVisible( false );
	    }
    }
    
    peer3DView = get3DView( plugin_interface, this, c3DViewSection, params );

    if ( channelTable != null ) {
	    channelTable.addSelectionListener(new SelectionAdapter() {
	      @Override
	      public void widgetSelected(SelectionEvent e) {
	        Table table = (Table)e.getSource();
	        //Check that at least an item is selected
	        //OSX lets you select nothing in the tree for example when a child is selected
	        //and you close its parent.
	        if(table.getSelection().length > 0) {
	          table.getSelection()[0].setFont(null);
	          showSection(table.getSelection()[0]);
	        }          
	      }
	    });
    }
  }
  
  private void
  disable3DView()
  {
	  disabled = true;
	  
	  peer3DView.delete();
	  
	  Utils.disposeComposite( panel, false );
	  
	  GridLayout gridLayout = new GridLayout();

	  gridLayout.numColumns = 1;
	  panel.setLayout(gridLayout);

	  data = new GridData(GridData.FILL_BOTH);
	  panel.setLayoutData(data);

	  new Label( panel, SWT.NULL );
	  
	  Label label = new Label( panel, SWT.WRAP );
	  label.setText( plugin.getMessageText( "view3d.only.one" ));
	  label.setLayoutData(new GridData( SWT.CENTER, SWT.CENTER, true, true ));
	  
	  new Label( panel, SWT.NULL );

	  panel.layout(true, true);
  }
  
  public Composite getComposite() {
    return panel;
  }

  public String getPluginViewName() {    
    return "3D View";
  }

  public String getFullTitle() {
    return "3D View";
  }
  
  protected void
  setDataSource(
	Download	download )
  {
	peer3DView.setDownload( download );
  }
  
  protected Download
  getDataSouce()
  {
	  return( peer3DView.getDownload());
  }
  
  private class Swarm3DDownloadListener implements DownloadListener
  {
    @Override
    public void stateChanged(Download download, int old_state, int new_state) {
		synchronized( activeDownloads ) {

			if(new_state == Download.ST_DOWNLOADING || new_state == Download.ST_SEEDING ) {
				if(!activeDownloads.contains(download)) {
					activeDownloads.add(download);
					displayDownload(download);
				}
			} else {
				if(activeDownloads.contains(download)) {
					activeDownloads.remove(download);
					disposeDownload(download);   
				}
			}
		}
    }

    @Override
    public void positionChanged(Download download,
                                int oldPosition, int newPosition) {}
  }
  
  private class Swarm3DDMListener implements DownloadManagerListener
  {
    private DownloadListener        download_listener;

    
    public Swarm3DDMListener() {
      download_listener = new Swarm3DDownloadListener();
      downloads = download_manager.getDownloads();
      for(int i=0;i<downloads.length;i++) {
    	  Download download = downloads[i];
    	  if(download.getState() == Download.ST_DOWNLOADING || download.getState() == Download.ST_SEEDING) {
    		  synchronized( activeDownloads ) {
    			  activeDownloads.add(download);
    		  }
    		  displayDownload(download);
    	  }
      }
    }

    @Override
    public void downloadAdded(final Download download) {
    	download.addListener( download_listener );
    }

    @Override
    public void downloadRemoved(final Download download) {
    	download.removeListener( download_listener ); 
      }
  }

  private void showSection(TableItem section) {
    
    Download download = (Download)section.getData("Download");

    if (download != null) {
      peer3DView.setDownload(download);
      updateHeader(section);
    }
  }
  
  private void updateHeader(TableItem section) {
    Download download = (Download)section.getData("Download");
    updateHeader(download);
  }  
  private void updateHeader(Download download) {
	    lHeader.setText("  3D View : " + download.getTorrent().getName());
  }
  
  public void delete() {   
//    TreeItem[] items = channelTree.getItems();
	  
	  peer3DView.delete();

    if(headerFont != null && ! headerFont.isDisposed()) {
      headerFont.dispose();
    }
  }
  
  private long	last_active_calc;
  
  public void refresh() {
	  if ( disabled ) {
		  return;
	  }
	  if ( view_id.equals( Plugin3D.VIEWID_MOST_ACTIVE )){
		  
		  long	now = SystemTime.getMonotonousTime();
		  
		  if ( now - last_active_calc >= 5*1000 ) {
			  
			  last_active_calc = now;
			  
			  synchronized( activeDownloads ) {
				  Download	fastest = null;
				  long		speed	= -1;
				  
				  for ( Download d: activeDownloads ) {
					  long tot = d.getStats().getDownloadAverage() + d.getStats().getUploadAverage();
					  
					  if ( tot > speed ) {
						  
						  speed 	= tot;
						  fastest	= d;
					  }
				  }
				  
				  if ( speed > 0 ) {
					  peer3DView.setDownload(fastest);
				      updateHeader(fastest); 
				  }
			  }
		  }
	  }
  }
  
	/**
	 * @param download
	 */
	private void disposeDownload(final Download download) {
		if( display != null && ! display.isDisposed() && channelTable != null && !channelTable.isDisposed()) {
		    display.asyncExec(new Runnable() {
		      @Override
		      public void run() {
		    	  if ( !channelTable.isDisposed()){
			    	items = channelTable.getItems();
			        for(int i = 0 ; i < items.length ; i++) {
			          if(items[i].getData("Download") == download) {
			            items[i].dispose();
			          }
			        }
		        }	
		      }
		    });    
		  }
	}

	/**
	 * @param download
	 */
	private void displayDownload(final Download download) {
		if( display != null && ! display.isDisposed() && channelTable != null && !channelTable.isDisposed()) {
		      display.asyncExec(new Runnable() {
		        @Override
		        public void run() {
		        	if ( !channelTable.isDisposed()){
			          item = new TableItem(channelTable,SWT.NONE);
			          item.setData("Download",download);
			          item.setText(download.getTorrent().getName());
			          fillTable();
		        	}
		        }
		      });    
		    }
	}
	
	private void fillTable() {
	    // Turn off drawing to avoid flicker
	    channelTable.setRedraw(false);
	    synchronized( activeDownloads ) {
		    Download[] dls = new Download[activeDownloads.size()];
		    Iterator iterator = activeDownloads.iterator();
		    int k = 0;
		    while( iterator.hasNext() ) {
		    	dls[k] = (Download)iterator.next();
		    	k++;
		    }
		    // We remove all the table entries, sort our
		    // rows, then add the entries
		    channelTable.removeAll();
			Arrays.sort(
					dls,
					new Comparator()
					{
						@Override
						public int
						compare(
							Object	o1,
							Object	o2 )
						{
							Download	d1 = (Download)o1;
							Download	d2 = (Download)o2;
	
							int	res = comparator.compare( "" + d1.getTorrent().getName(),  "" + d2.getTorrent().getName() );
							
							return(  res );
						}
					});
		    for (int i = 0; i<dls.length; i++) {
		    	item = new TableItem(channelTable,SWT.NONE);
		        item.setData("Download",dls[i]);
		        item.setText(dls[i].getTorrent().getName());          
		    }
	    }
	    // Turn drawing back on
	    channelTable.setRedraw(true);
	    name.pack();
	  }
 
}
  
