package com.examw.netschool;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.examw.netschool.app.Constant;
import com.examw.netschool.util.DownloadFactory;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 下载完成Fragment。
 * 
 * @author jeasonyoung
 * @since 2015年9月11日
 */
public class DownloadByFragmentFinish extends Fragment {
	private static final String TAG = "downloadFinish";
	private LinearLayout nodataView;
	private final List<DownloadFactory.DownloadItemConfig> dataSource;
	private final FinishAdapter adapter;

	/**
	 * 构造函数。
	 */
	public DownloadByFragmentFinish(){
		Log.d(TAG, "初始化...");
		this.dataSource = new ArrayList<>();
		this.adapter = new FinishAdapter(this.dataSource);
	}

	/*
	 * 重载创建View
	 * @see android.support.v4.app.Fragment#onCreateView(android.view.LayoutInflater, android.view.ViewGroup, android.os.Bundle)
	 */
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		//super.onCreateView(inflater, container, savedInstanceState);
		Log.d(TAG, "重载创建View...");
		//加载列表布局文件
		final View view = inflater.inflate(R.layout.activity_download_finish, container, false);
		if(view != null) {
            //没有数据
            this.nodataView = (LinearLayout) view.findViewById(R.id.nodata_view);
            if(this.nodataView != null) this.nodataView.setVisibility(View.VISIBLE);
            //列表
            final ListView listView = (ListView) view.findViewById(R.id.download_listview_finish);
            if(listView != null) {
                //长按弹出删除已下载的课程资源
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
		Log.d(TAG, "重载启动，异步加载数据...");
		//绑定下载事件监听。
		DownloadFactory.getInstance()
				.addActionListener(this.onDownloadActionListener);
		//异步加载数据
		new AsyncLoadData().execute((Void)null);
	}

    @Override
    public void onDestroy() {
        DownloadFactory.getInstance()
                .removeActionListener(this.onDownloadActionListener);
        super.onDestroy();
    }

    //长按删除下载
	private OnItemLongClickListener onItemLongClickListener = new OnItemLongClickListener() {
		/*
		 * 长按删除事件处理。
		 * @see android.widget.AdapterView.OnItemLongClickListener#onItemLongClick(android.widget.AdapterView, android.view.View, int, long)
		 */
		@Override
		public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
			Log.d(TAG, "下载["+position+"]删除...");
			if(dataSource.size() > position){
				//获取下载课程
				final DownloadFactory.DownloadItemConfig download = dataSource.get(position);
				if(download != null){
					//取消下载二次确认
					new AlertDialog.Builder(getActivity()).setIcon(android.R.drawable.ic_dialog_alert)
					.setTitle(R.string.download_group_finish_delete_title).setMessage(R.string.download_group_finish_delete_msg)
					.setNegativeButton(R.string.download_group_finish_delete_btn_cancel, new DialogInterface.OnClickListener(){
						/*
						 * 取消退出。
						 * @see android.content.DialogInterface.OnClickListener#onClick(android.content.DialogInterface, int)
						 */
						@Override
						public void onClick(DialogInterface dialog, int which) {
							Log.d(TAG, "取消删除");
							dialog.dismiss();
						}
					})
					.setPositiveButton(R.string.download_group_finish_delete_btn_submit, new DialogInterface.OnClickListener(){
						/*
						 * 确定取消下载。
						 * @see android.content.DialogInterface.OnClickListener#onClick(android.content.DialogInterface, int)
						 */
						@Override
						public void onClick(DialogInterface dialog, int which) {
							Log.d(TAG, "删除下载课程资源[" + download + "]...");
							//删除
							DownloadFactory.getInstance()
									.deleteDownload(download);
						}
					}).show();
					return true;
				}
			}
			return false;
		}
	};

	/**
	 * 下载事件监听器。
	 */
	private DownloadFactory.DownloadActionListener onDownloadActionListener = new DownloadFactory.DownloadActionListener(){
		@Override
		public void waiting(DownloadFactory.DownloadItemData data) {

		}

		@Override
		public void start(DownloadFactory.DownloadItemData data) {

		}

		@Override
		public void failedDownload(DownloadFactory.DownloadItemData data, Exception e) {

		}

		@Override
		public void updateProgress(DownloadFactory.DownloadItemData data) {

		}

		@Override
		public void finishedDownload(DownloadFactory.DownloadItemData data) {
			//重新刷新数据
			new AsyncLoadData().execute((Void)null);
		}
	};

	//异步加载数据
	private class AsyncLoadData extends AsyncTask<Void, Void, List<DownloadFactory.DownloadItemConfig>>{
        private static final String TAG = "asyncLoadData";

		/*
		 * 后台线程处理。
		 * @see android.os.AsyncTask#doInBackground(java.lang.Object[])
		 */
		@Override
		protected List<DownloadFactory.DownloadItemConfig> doInBackground(Void... params) {
			try{
				Log.d(TAG, "异步线程加载数据...");
				//加载已下载完成的数据
				return DownloadFactory.getInstance().loadFinishedFiles();
			}catch(Exception e){
				Log.e(TAG, "异步线程加载数据异常:" + e.getMessage(), e);
			}
			return null;
		}

		/*
		 * 前台主线程处理。
		 * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
		 */
		@Override
		protected void onPostExecute(List<DownloadFactory.DownloadItemConfig> result) {
			//移除数据源
			dataSource.clear();
			//
			if(result != null && result.size() > 0){
				//填充数据
				dataSource.addAll(result);
				//隐藏无数据View
				nodataView.setVisibility(View.GONE);
			}else{
				//显示无数据View
				nodataView.setVisibility(View.VISIBLE);
			}
			//通知适配器更新
			adapter.notifyDataSetChanged();
		}
	}

	//数据适配器
	private class FinishAdapter extends BaseAdapter{
		private static final String TAG = "finishAdapter";
		private final List<DownloadFactory.DownloadItemConfig> downloads;
		/**
		 * 构造函数。
		 * @param downloads
         * 下载数据列表。
		 */
		public FinishAdapter(List<DownloadFactory.DownloadItemConfig> downloads){
			Log.d(TAG, "初始化...");
			this.downloads = downloads;
		}

		/*
		 * 获取数据量。
		 * @see android.widget.Adapter#getCount()
		 */
		@Override
		public int getCount() {
			return (this.downloads == null) ? 0 : this.downloads.size();
		}
		/*
		 * 获取数据。
		 * @see android.widget.Adapter#getItem(int)
		 */
		@Override
		public Object getItem(int position) {
			if(this.downloads != null && this.downloads.size() > position){
				return this.downloads.get(position);
			}
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
		 * 获取数据行View
		 * @see android.widget.Adapter#getView(int, android.view.View, android.view.ViewGroup)
		 */
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			Log.d(TAG, "获取数据行View..." + position);
			ViewHolder viewHolder;
			if(convertView == null){
				Log.d(TAG, "新建行..." + position);
				//加载行布局文件
				convertView = LayoutInflater.from(getActivity()).inflate(R.layout.activity_download_finish_item, parent, false);
				//初始化
				viewHolder = new ViewHolder(convertView);
				//缓存
				convertView.setTag(viewHolder);
			}else{
				//重用行
				Log.d(TAG, "重用行..." + position);
				viewHolder = (ViewHolder)convertView.getTag();
			}
			//加载数据
			viewHolder.loadData((DownloadFactory.DownloadItemConfig)this.getItem(position));
			//返回
			return convertView;
		}
	}
	//数据行View
	private class ViewHolder implements OnClickListener{
		private static final String TAG = "viewHolder";
		private TextView tvTitle;
		private View btnPlay;
		private String lessonId;

		/**
		 * 构造函数。
		 * @param convertView
         * 视图。
		 */
		public ViewHolder(View convertView){
			Log.d(TAG, "初始化...");
			//加载标题
			this.tvTitle = (TextView)convertView.findViewById(R.id.tv_title);
			//播放按钮
			this.btnPlay = convertView.findViewById(R.id.btn_play);
			this.btnPlay.setOnClickListener(this);
		}

		/**
		 * 加载数据。
		 * @param data
         * 数据。
		 */
		public void loadData(DownloadFactory.DownloadItemConfig data){
			Log.d(TAG, "加载数据..." + data);
			if(data == null) return;
			//获取课程资源ID。
			this.lessonId = data.getId();
			//设置课程资源名称
			this.tvTitle.setText(data.getName());
		}

		/*
		 * 播放按钮。
		 * @see android.view.View.OnClickListener#onClick(android.view.View)
		 */
		@Override
		public void onClick(View v) {
			Log.d(TAG, "播放按钮事件处理..." + v);
			if(StringUtils.isBlank(this.lessonId)) return;
			//
			final Intent intent = new Intent(getActivity(), NativePlayActivity.class);
			intent.putExtra(Constant.CONST_LESSON_ID, this.lessonId);
			getActivity().startActivity(intent);
		}
	}
}