package mega.privacy.android.app;

import java.util.ArrayList;

import mega.privacy.android.app.utils.ThumbnailUtils;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaNode;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;


public class MegaExplorerAdapter extends BaseAdapter {
	
	final public static int CLOUD_EXPLORER = 0;
	final public static int INCOMING_SHARES_EXPLORER = 1;

	Context context;
	MegaApiAndroid megaApi;

	int positionClicked;
	ArrayList<Integer> imageIds;
	ArrayList<String> names;
	ArrayList<MegaNode> nodes;
	
	private ArrayList<Long> disabledNodes;
	
	long parentHandle = -1;
	boolean selectFile = false;
	int caller;
	
	ListView listFragment;
	ImageView emptyImageViewFragment;
	TextView emptyTextViewFragment;
	
	/*public static view holder class*/
    public class ViewHolderExplorer {
    	public ImageView imageView;
    	public TextView textViewFileName;
    	public TextView textViewFileSize;
    	public TextView textViewUpdated;
    	public RelativeLayout itemLayout;
    	public int currentPosition;
    	public long document;
    }
	
	public MegaExplorerAdapter(Context _context, ArrayList<MegaNode> _nodes, long _parentHandle, ListView listView, ImageView emptyImageView, TextView emptyTextView, boolean selectFile){
		this.context = _context;
		this.nodes = _nodes;
		this.parentHandle = _parentHandle;
		this.listFragment = listView;
		this.emptyImageViewFragment = emptyImageView;
		this.emptyTextViewFragment = emptyTextView;
		this.selectFile = selectFile;
		this.positionClicked = -1;
		this.imageIds = new ArrayList<Integer>();
		this.names = new ArrayList<String>();
		
		if (megaApi == null){
			megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
		}
	}
	
	@Override
	public int getCount() {
		return nodes.size();
	}

	@Override
	public Object getItem(int position) {
		return nodes.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		
		listFragment = (ListView) parent;
		
		final int _position = position;
		
		ViewHolderExplorer holder = null;
		
		Display display = ((Activity)context).getWindowManager().getDefaultDisplay();
		DisplayMetrics outMetrics = new DisplayMetrics ();
	    display.getMetrics(outMetrics);
	    float density  = ((Activity)context).getResources().getDisplayMetrics().density;
		
	    float scaleW = Util.getScaleW(outMetrics, density);
	    float scaleH = Util.getScaleH(outMetrics, density);
		
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		if (convertView == null) {
			convertView = inflater.inflate(R.layout.item_file_explorer, parent, false);
			holder = new ViewHolderExplorer();
			holder.itemLayout = (RelativeLayout) convertView.findViewById(R.id.file_explorer_item_layout);
			holder.imageView = (ImageView) convertView.findViewById(R.id.file_explorer_thumbnail);
			holder.textViewFileName = (TextView) convertView.findViewById(R.id.file_explorer_filename);
			holder.textViewFileName.getLayoutParams().height = RelativeLayout.LayoutParams.WRAP_CONTENT;
			holder.textViewFileName.getLayoutParams().width = Util.px2dp((225*scaleW), outMetrics);
			holder.textViewFileSize = (TextView) convertView.findViewById(R.id.file_explorer_filesize);
			holder.textViewUpdated = (TextView) convertView.findViewById(R.id.file_explorer_updated);
			
			convertView.setTag(holder);
		}
		else{
			holder = (ViewHolderExplorer) convertView.getTag();
		}
		
		holder.currentPosition = position;
		
		MegaNode node = (MegaNode) getItem(position);
		holder.document = node.getHandle();
		Bitmap thumb = null;
		
		holder.textViewFileName.setText(node.getName());
			
		if (node.isFolder()){			

			Util.setViewAlpha(holder.imageView, 1);
			holder.textViewFileName.setTextColor(context.getResources().getColor(android.R.color.black));
			
			if (disabledNodes != null){
				if (disabledNodes.contains(node.getHandle())){
					Util.setViewAlpha(holder.imageView,  .4f);
					holder.textViewFileName.setTextColor(context.getResources().getColor(R.color.text_secondary));
				}
			}
			holder.textViewFileSize.setText(getInfoFolder(node));
			holder.imageView.setImageResource(R.drawable.ic_folder_list);
		}
		else{
			if(selectFile)
			{
				Util.setViewAlpha(holder.imageView, 1);
				holder.textViewFileName.setTextColor(context.getResources().getColor(android.R.color.black));	
			}
			else{
				Util.setViewAlpha(holder.imageView, .4f);
				holder.textViewFileName.setTextColor(context.getResources().getColor(R.color.text_secondary));
			}			
			
			long nodeSize = node.getSize();
			holder.textViewFileSize.setText(Util.getSizeString(nodeSize));
			holder.imageView.setImageResource(MimeTypeList.typeForName(node.getName()).getIconResourceId());
			
			if (node.hasThumbnail()){
				thumb = ThumbnailUtils.getThumbnailFromCache(node);
				if (thumb != null){
					holder.imageView.setImageBitmap(thumb);
				}
				else{
					thumb = ThumbnailUtils.getThumbnailFromFolder(node, context);
					if (thumb != null){
						holder.imageView.setImageBitmap(thumb);
					}
					else{ 
						try{
							thumb = ThumbnailUtils.getThumbnailFromMegaExplorer(node, context, holder, megaApi, this);
						}
						catch(Exception e){} //Too many AsyncTasks
						
						if (thumb != null){
							holder.imageView.setImageBitmap(thumb);
						}
					}
				}
			}
			else{
				thumb = ThumbnailUtils.getThumbnailFromCache(node);
				if (thumb != null){
					holder.imageView.setImageBitmap(thumb);
				}
				else{
					thumb = ThumbnailUtils.getThumbnailFromFolder(node, context);
					if (thumb != null){
						holder.imageView.setImageBitmap(thumb);
					}
					else{ 
						try{
							ThumbnailUtils.createThumbnailExplorer(context, node, holder, megaApi, this);
						}
						catch(Exception e){} //Too many AsyncTasks
					}
				}			
			}
		}
		
		long nodeDate = node.getCreationTime();
		if (nodeDate != 0){
			try{ 
				holder.textViewUpdated.setText(Util.getDateString(nodeDate));
			}
			catch(Exception ex) {
				holder.textViewUpdated.setText(""); 
			}
		}
		else{
			holder.textViewUpdated.setText("");
		}
		
		return convertView;
	}
	
	private String getInfoFolder (MegaNode n){
		int numFolders = megaApi.getNumChildFolders(n);
		int numFiles = megaApi.getNumChildFiles(n);
		
		String info = "";
		if (numFolders > 0){
			info = numFolders +  " " + context.getResources().getQuantityString(R.plurals.general_num_folders, numFolders);
			if (numFiles > 0){
				info = info + ", " + numFiles + " " + context.getResources().getQuantityString(R.plurals.general_num_files, numFiles);
			}
		}
		else {
			info = numFiles +  " " + context.getResources().getQuantityString(R.plurals.general_num_files, numFiles);
		}
		
		return info;
	}
	
	@Override
	public boolean isEnabled(int position) {
		if (nodes.size() == 0){
			return false;
		}
		
		MegaNode document = nodes.get(position);
		if (document.isFile()){
			if(selectFile){
				return true;
			}
			else{
				return false;
			}
		}
		else{
			if (disabledNodes != null) {
				if (disabledNodes.contains(document.getHandle())){
					return false;
				}
			}
		}
		
		return true;
	}

	public int getPositionClicked (){
    	return positionClicked;
    }
    
    public void setPositionClicked(int p){
    	positionClicked = p;
    }
	
	public void setNodes(ArrayList<MegaNode> nodes){
		this.nodes = nodes;
		positionClicked = -1;	
		notifyDataSetChanged();
	}
	
	public long getParentHandle(){
		return parentHandle;
	}
	
	public void setParentHandle(long parentHandle){
		this.parentHandle = parentHandle;
	}
	
	/*
	 * Set provided nodes disabled
	 */
	public void setDisableNodes(ArrayList<Long> disabledNodes) {
		this.disabledNodes = disabledNodes;
	}

	public boolean isSelectFile() {
		return selectFile;
	}

	public void setSelectFile(boolean selectFile) {
		this.selectFile = selectFile;
	}

}
