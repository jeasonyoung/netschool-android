package com.examw.netschool;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.examw.netschool.util.DownloadFactory;

import org.apache.commons.lang3.StringUtils;

import java.lang.ref.WeakReference;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
/**
 * 下载中Fragment。
 * 
 * @author jeasonyoung
 * @since 2015年9月11日
 */
public class DownloadByFragmentDowning extends Fragment {
	private static final String TAG = "downloadDowning";
	private static final int MSG_TYPE_WAITING = 0;//消息类型－排队等待
	private static final int MSG_TYPE_START = 1;//消息类型－开始下载
	private static final int MSG_TYPE_FAILED = 2;//消息类型－下载失败
	private static final int MSG_TYPE_UPDATE = 3;//消息类型－更新进度
	private static final int MSG_TYPE_FINISHED = 4;//消息类型－下载完成

	private static final String BUNDLE_DATA_KEY = "data";//传递数据KEY
	private static final String BUNDLE_MSG_KEY = "msg";//传递文本数据KEY

	private LinearLayout nodataView;
	private final List<DownloadFactory.DownloadItemData> dataSource;
	private final DowningAdapter adapter;
	private DowningUIHandler handler;
	private ListView listView;
	/**
	 * 构造函数。
	 */
	public DownloadByFragmentDowning(){
		Log.d(TAG, "初始化...");
		this.dataSource = new ArrayList<>();
		this.adapter = new DowningAdapter(this.dataSource);
	}

	/*
	 * 重载创建视图。
	 * @see android.support.v4.app.Fragment#onCreateView(android.view.LayoutInflater, android.view.ViewGroup, android.os.Bundle)
	 */
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		//super.onCreateView(inflater, container, savedInstanceState);
		Log.d(TAG, "重载创建视图....");
		//加载视图
		final View view = inflater.inflate(R.layout.activity_download_downing, container, false);
		if(view != null) {
            //没有数据
            this.nodataView = (LinearLayout)view.findViewById(R.id.nodata_view);
            if(this.nodataView != null) this.nodataView.setVisibility(View.VISIBLE);

            //列表
            this.listView = (ListView)view.findViewById(R.id.download_listview_downing);
            if(this.listView != null) {
                //长按弹出取消下载的PopupWindow
                listView.setOnItemLongClickListener(this.onItemLongClickListener);
                //设置数据适配器
                listView.setAdapter(this.adapter);
            }
        }
		//返回
		return view;
	}
	/*
	 * 重载启动。
	 * @see android.support.v4.app.Fragment#onStart()
	 */
	@Override
	public void onStart() {
		super.onStart();
		Log.d(TAG, "重载启动...");
		this.handler = new DowningUIHandler(this);
		DownloadFactory.getInstance()
				.addActionListener(this.onDownloadActionListener);
		//异步加载数据
		this.reloadData();
	}

	@Override
	public void onDestroy() {
		DownloadFactory.getInstance()
				.removeActionListener(this.onDownloadActionListener);
		super.onDestroy();
	}

	//异步加载数据。
	private void reloadData(){
		Log.d(TAG, "异步加载数据...");
		new AsyncLoadData().execute((Void) null);
	}

	//下载长按取消
	private OnItemLongClickListener onItemLongClickListener = new OnItemLongClickListener(){
		@Override
		public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
			Log.d(TAG, "下载["+position+"]取消...");
			if(dataSource.size() > position){
				//获取下载课程
				final DownloadFactory.DownloadItemData download = dataSource.get(position);
				if(download != null){
					//取消下载二次确认
					new AlertDialog.Builder(getActivity()).setIcon(android.R.drawable.ic_dialog_alert)
					.setTitle(R.string.download_group_downing_cancel_title).setMessage(R.string.download_group_downing_cancel_msg)
					.setNegativeButton(R.string.download_group_downing_cancel_btn_cancel, new DialogInterface.OnClickListener(){
						/*
						 * 取消退出。
						 * @see android.content.DialogInterface.OnClickListener#onClick(android.content.DialogInterface, int)
						 */
						@Override
						public void onClick(DialogInterface dialog, int which) {
							Log.d(TAG, "取消退出");
							dialog.dismiss();
						}
					})
					.setPositiveButton(R.string.download_group_downing_cancel_btn_submit, new DialogInterface.OnClickListener(){
						/*
						 * 确定取消下载。
						 * @see android.content.DialogInterface.OnClickListener#onClick(android.content.DialogInterface, int)
						 */
						@Override
						public void onClick(DialogInterface dialog, int which) {
							Log.d(TAG, "取消下载课程资源["+download+"]...");
							//取消下载
							DownloadFactory.getInstance()
									.cancelDownload(download);
						}
					}).show();
					return true;
				}
			}
			return false;
		}
	};


	private DownloadFactory.DownloadActionListener onDownloadActionListener = new DownloadFactory.DownloadActionListener(){

		@Override
		public void waiting(DownloadFactory.DownloadItemData data) {
			final Message msg = new Message();
			msg.what = MSG_TYPE_WAITING;
			Bundle bundle = new Bundle();
			bundle.putSerializable(BUNDLE_DATA_KEY, data);
			msg.setData(bundle);
			handler.sendMessage(msg);
		}

		@Override
		public void start(DownloadFactory.DownloadItemData data) {
			final Message msg = new Message();
			msg.what = MSG_TYPE_START;
			Bundle bundle = new Bundle();
			bundle.putSerializable(BUNDLE_DATA_KEY, data);
			msg.setData(bundle);
			handler.sendMessage(msg);
		}

		@Override
		public void failedDownload(DownloadFactory.DownloadItemData data, Exception e) {
			final Message msg = new Message();
			msg.what = MSG_TYPE_FAILED;
			Bundle bundle = new Bundle();
			bundle.putSerializable(BUNDLE_DATA_KEY, data);
			bundle.putString(BUNDLE_MSG_KEY, e.getMessage());
			msg.setData(bundle);
			handler.sendMessage(msg);
		}

		@Override
		public void updateProgress(DownloadFactory.DownloadItemData data) {
			final Message msg = new Message();
			msg.what = MSG_TYPE_UPDATE;
			Bundle bundle = new Bundle();
			bundle.putSerializable(BUNDLE_DATA_KEY, data);
			msg.setData(bundle);
			handler.sendMessage(msg);
		}

		@Override
		public void finishedDownload(DownloadFactory.DownloadItemData data) {
			final Message msg = new Message();
			msg.what = MSG_TYPE_FINISHED;
			Bundle bundle = new Bundle();
			bundle.putSerializable(BUNDLE_DATA_KEY, data);
			msg.setData(bundle);
			handler.sendMessage(msg);
		}
	};

	//异步加载数据。
	private class AsyncLoadData extends AsyncTask<Void, Void, List<DownloadFactory.DownloadItemData>>{

		/*
		 * 后台异步线程处理.
		 * @see android.os.AsyncTask#doInBackground(java.lang.Object[])
		 */
		@Override
		protected List<DownloadFactory.DownloadItemData> doInBackground(Void... params) {
			try{
				Log.d(TAG, "异步加载列表数据...");
				return DownloadFactory.getInstance().getDowning();
			}catch(Exception e){
				Log.e(TAG, " 异步加载列表数据异常:" + e.getMessage(), e);
			}
			return null;
		}

		/*
		 * 前台主线程处理
		 * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
		 */
		@Override
		protected void onPostExecute(List<DownloadFactory.DownloadItemData> result) {
			Log.d(TAG, "前台主线程更新数据...");
			//移除数据源
			dataSource.clear();
			//
			if(result != null && result.size() > 0){
				//填充数据
				dataSource.addAll(result);
			}
			//是否显示无数据View
			nodataView.setVisibility(dataSource.size() > 0 ? View.GONE : View.VISIBLE);
			//通知适配器更新
			adapter.notifyDataSetChanged();
		}
	}

	//数据适配器。
	private class DowningAdapter extends BaseAdapter{
		private static final String TAG = "downingAdapter";
		private final List<DownloadFactory.DownloadItemData> downloads;

		/**
		 * 构造函数。
		 * @param downloads
		 * 下载数据列表。
		 */
		public DowningAdapter(List<DownloadFactory.DownloadItemData> downloads){
			this.downloads = downloads;
		}

		/*
		 * 获取数据量。
		 * @see android.widget.Adapter#getCount()
		 */
		@Override
		public int getCount() {
			return (downloads == null) ? 0 : downloads.size();
		}

		/*
		 * 获取数据。
		 * @see android.widget.Adapter#getItem(int)
		 */
		@Override
		public Object getItem(int position) {
			if(downloads != null && downloads.size() > position)
				return downloads.get(position);
			return null;
		}

		/*
		 * 获取数据ID。
		 * @see android.widget.Adapter#getItemId(int)
		 */
		@Override
		public long getItemId(int position) {
			return position;
		}
		/*
		 * 获取数据行View。
		 * @see android.widget.Adapter#getView(int, android.view.View, android.view.ViewGroup)
		 */
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			Log.d(TAG, "创建数据行["+position+"]...");
			ViewHolder viewHolder;
			if(convertView == null){
				Log.d(TAG, "创建新行..." + position);
				//加载布局文件
				convertView = LayoutInflater.from(getActivity()).inflate(R.layout.activity_download_downing_item, parent, false);
				//初始化
				viewHolder = new ViewHolder(convertView);
				//缓存
				convertView.setTag(viewHolder);
			}else{
				Log.d(TAG, "重用行:" + position);
				//加载控件
				viewHolder = (ViewHolder)convertView.getTag();
			}
			//加载数据
			viewHolder.loadData((DownloadFactory.DownloadItemData)this.getItem(position));
			//返回View
			return convertView;
		}
	}

	//数据行View
	private class ViewHolder implements OnClickListener{
		private TextView tvTitle,tvMsg,tvPercent;
		private ProgressBar progressBar;
		private ImageButton btnPause;
		private DownloadFactory.DownloadItemData data;

		/**
		 * 构造函数。
		 * @param convertView
		 * 视图
		 */
        @SuppressWarnings("deprecation")
		public ViewHolder(View convertView){
			//标题
			this.tvTitle = (TextView)convertView.findViewById(R.id.tv_title);
			//进度条
			this.progressBar = (ProgressBar)convertView.findViewById(R.id.progress);
			//消息
			this.tvMsg = (TextView)convertView.findViewById(R.id.tv_msg);
			//百分比
			this.tvPercent = (TextView)convertView.findViewById(R.id.tv_percent);
			//按钮
			this.btnPause = (ImageButton)convertView.findViewById(R.id.btn_pause);
			//设置按钮事件处理
			this.btnPause.setOnClickListener(this);
		}

		/**
		 * 加载数据。
		 * @param download
		 * 下载数据。
		 */
        @SuppressWarnings("deprecation")
		public void loadData(DownloadFactory.DownloadItemData download){
			Log.d(TAG, "加载行数据...");
			if((this.data = download) == null) return;
			//标题
			this.tvTitle.setText(download.getName());

			float per = 0;
			if(download.getSize() > 0){
				per = (((float)download.getReceivedSize() / (float)download.getSize()) * 100);
			}

			//进度条
			this.progressBar.setProgress((int)per);
			//百分比
			this.tvPercent.setText(String.format("%s%%",new DecimalFormat(".0").format(per)));

			//资源
			final Resources res = getActivity().getResources();
            if(res != null) {
                //
                if (per >= 100) {
                    this.tvMsg.setText("已完成!");
                    this.btnPause.setVisibility(View.GONE);
                } else if (download.isWaiting()) {//排队等待
                    this.tvMsg.setText("排队等待");
                    this.btnPause.setVisibility(View.GONE);
                } else if (download.isDownloading()) {//下载中
                    this.tvMsg.setText("下载中");
                    final Drawable top = res.getDrawable(R.drawable.download_group_downing_item_btn_pause_icon);
                    if (top != null) {
                        top.setBounds(0, 0, top.getMinimumWidth(), top.getMinimumHeight());
                        this.btnPause.setImageDrawable(top);
                    }
                    this.btnPause.setContentDescription(res.getText(R.string.download_group_downing_item_btn_start));
                    this.btnPause.setVisibility(View.VISIBLE);
                    this.btnPause.setEnabled(true);
                } else {
                    final Drawable top = res.getDrawable(R.drawable.download_group_downing_item_btn_continue_icon);
                    if (top != null) {
                        top.setBounds(0, 0, top.getMinimumWidth(), top.getMinimumHeight());
                        this.btnPause.setImageDrawable(top);
                    }
                    final CharSequence title = res.getText(R.string.download_group_downing_item_btn_pause);
                    this.btnPause.setContentDescription(title);
                    this.btnPause.setVisibility(View.VISIBLE);
                    this.btnPause.setEnabled(true);
                    this.tvMsg.setText(title);
                }
            }
		}

		/*
		 *按钮事件处理。
		 * @see android.view.View.OnClickListener#onClick(android.view.View)
		 */
		@Override
		public void onClick(View v) {
			try{
				Log.d(TAG, "按钮点击事件处理..." + v + "["+ this.data+"]");
				if(this.data == null) return;
				final DownloadFactory factory = DownloadFactory.getInstance();
				if(this.data.isDownloading()){//暂停
					factory.pauseDownload(this.data);
				}else{//继续
					factory.continueDownload(this.data);
				}
			}catch(Exception e){
				Log.e(TAG, "按钮点击操作时异常:" + e.getMessage(), e);
			}
		}
	}

	/**
	 * 下载UI更新处理.
	 */
	private static class DowningUIHandler extends Handler{
		private final WeakReference<DownloadByFragmentDowning> fragmentDowningRef;

		/**
		 * 构造函数。
		 * @param fragmentDowning
		 * 当前对象。
		 */
		public DowningUIHandler(DownloadByFragmentDowning fragmentDowning){
			this.fragmentDowningRef = new WeakReference<>(fragmentDowning);
		}
		/*
		 * 重载消息处理。
		 * @see android.os.Handler#handleMessage(android.os.Message)
		 */
        @SuppressWarnings("deprecation")
		@Override
		public void handleMessage(Message msg) {
			//获取当前对象
			final DownloadByFragmentDowning f = this.fragmentDowningRef.get();
			if(f == null) return;
			//完成下载数据
			if(msg.what == MSG_TYPE_FINISHED){
				//刷新数据
				f.reloadData();
				return;
			}
			//获取数据
			final Bundle bundle = msg.getData();
			if(bundle == null) return;
			DownloadFactory.DownloadItemData data = (DownloadFactory.DownloadItemData)bundle.getSerializable(BUNDLE_DATA_KEY);
			if(data == null) return;

			//数据所在数据源中索引
			int pos = -1;
			if(f.dataSource != null && f.dataSource.size() > 0){
				pos = f.dataSource.indexOf(data);
			}
			//获取列表
			final ListView listView = f.listView;
			if(listView == null) return;
			final int first = listView.getFirstVisiblePosition(),
					  last = listView.getLastVisiblePosition();
			if(pos >= first && pos <= last){//数据可见
				//获取索引下View
				final View view = listView.getChildAt(pos);
				if(view == null) return;
				final ViewHolder holder = (ViewHolder)view.getTag();
				if(holder == null) return;
				//
				switch (msg.what){
					case MSG_TYPE_WAITING:{//排队等待
						holder.loadData(data);
						break;
					}
					case MSG_TYPE_START:{//开始下载
						holder.loadData(data);
						break;
					}
					case MSG_TYPE_UPDATE:{//下载进度更新
						holder.loadData(data);
						if(!data.isDownloading() && !data.isWaiting()){
							final Resources res = f.getActivity().getResources();
							final Drawable top = res.getDrawable(R.drawable.download_group_downing_item_btn_continue_icon);
							if(top != null){
								top.setBounds(0, 0, top.getMinimumWidth(), top.getMinimumHeight());
								holder.btnPause.setImageDrawable(top);
							}
                            final CharSequence title = res.getText(R.string.download_group_downing_item_btn_pause);
							holder.btnPause.setContentDescription(title);
							holder.btnPause.setVisibility(View.VISIBLE);
							holder.btnPause.setEnabled(true);
                            holder.tvMsg.setText(title);
						}
						break;
					}
					case MSG_TYPE_FAILED:{//下载失败
						holder.loadData(data);
						holder.tvMsg.setText("下载失败");
						final Resources res = f.getActivity().getResources();
						final Drawable top = res.getDrawable(R.drawable.download_group_downing_item_retry);
						if(top != null){
							top.setBounds(0, 0, top.getMinimumWidth(), top.getMinimumHeight());
							holder.btnPause.setImageDrawable(top);
						}
						holder.btnPause.setContentDescription(res.getText(R.string.download_group_downing_item_btn_start));
						holder.btnPause.setVisibility(View.VISIBLE);
						holder.btnPause.setEnabled(true);
						//
						final String text = bundle.getString(BUNDLE_MSG_KEY);
						if(StringUtils.isNotBlank(text)){
							holder.tvMsg.setText(text);
							Toast.makeText(f.getActivity(), text, Toast.LENGTH_SHORT).show();
						}
						break;
					}
				}
			}
		}
	}
}