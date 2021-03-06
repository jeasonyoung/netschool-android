package com.examw.netschool.ch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.examw.netschool.app.AppContext;
import com.examw.netschool.app.Constant;
import com.examw.netschool.dao.LessonDao;
import com.examw.netschool.model.JSONCallback;
import com.examw.netschool.model.Lesson;
import com.examw.netschool.util.APIUtils;
import com.examw.netschool.R;
import com.examw.netschool.util.DownloadFactory;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * 课程资源列表Activity.
 * 
 * @author jeasonyoung
 * @since 2015年9月5日
 */
public class MyCourseLessonActivity extends Activity {
	private static final String TAG = "MyCourseLessonActivity";
	
	private String classId,className;
	private LinearLayout nodataView;
	
	private final List<Lesson> lessons;
	private final LessonAdapter adapter;
	/**
	 * 构造函数。
	 */
	public MyCourseLessonActivity(){
		Log.d(TAG, "初始化...");
		this.lessons = new ArrayList<Lesson>();
		this.adapter = new LessonAdapter(this.lessons);
	}
	/*
	 * 重载创建。
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(TAG, "重载创建...");
		//设置布局文件
		this.setContentView(R.layout.activity_my_course_lesson);
		//获取传递数据
		final Intent intent = this.getIntent();
		if(intent != null){
			//设置班级ID
			this.classId = intent.getStringExtra(Constant.CONST_CLASS_ID);
			//设置班级名称
			this.className = intent.getStringExtra(Constant.CONST_CLASS_NAME);
		}
		//返回
		final View btnReturn = this.findViewById(R.id.btn_return);
		//设置点击事件处理
		btnReturn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Log.d(TAG, "返回事件处理...");
				finish();
			}
		});
		//top标题
		final TextView tvTopTitle = (TextView)this.findViewById(R.id.top_title);
		if(tvTopTitle != null){
			tvTopTitle.setText(className);
		}
		//无数据View
		this.nodataView = (LinearLayout)this.findViewById(R.id.nodata_view);
		//数据列表
		final ListView listView = (ListView)this.findViewById(R.id.list_course_lesson);
		//设置数据适配器
		listView.setAdapter(this.adapter);
		//
		//底部按钮事件处理
		//下载课程
		final View btnDownload = this.findViewById(R.id.btn_download);
		//设置点击事件处理
		btnDownload.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Log.d(TAG, "下载课程点击处理..." + v);
				//初始化意图
				final Intent intent = new Intent(MyCourseLessonActivity.this, DownloadActivity.class);
				//设置索引
				intent.putExtra(DownloadActivity.CONST_FRAGMENT_INDEX, DownloadActivity.CONST_FRAGMENT_DOWNING);
				//发送意图
				startActivity(intent);
			}
		});
		//离线课程
		final View btnOffline = this.findViewById(R.id.btn_offline);
		//设置点击事件处理
		btnOffline.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Log.d(TAG, "离线课程点击处理..." + v);
				//初始化意图
				final Intent intent = new Intent(MyCourseLessonActivity.this, DownloadActivity.class);
				//设置索引
				intent.putExtra(DownloadActivity.CONST_FRAGMENT_INDEX, DownloadActivity.CONST_FRAGMENT_FINISH);
				//发送意图
				startActivity(intent);
			}
		});
		//播放记录
		final View btnRecord = this.findViewById(R.id.btn_record);
		//设置点击事件处理
		btnRecord.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Log.d(TAG, "播放记录点击处理..." + v);
				//发送意图
				startActivity(new Intent(MyCourseLessonActivity.this, PlayRecordActivity.class));
			}
		});
	}
	/*
	 * 重载启动。
	 * @see android.app.Activity#onStart()
	 */
	@Override
	protected void onStart() {
		super.onStart();
		Log.d(TAG, "重载启动...");
		//异步加载数据
		new AsyncTask<Void, Void, List<Lesson>>() {
			private String msg;
			/*
			 * 后台线程加载数据。
			 * @see android.os.AsyncTask#doInBackground(java.lang.Object[])
			 */
			@Override
			protected List<Lesson> doInBackground(Void... params) {
				try {
					Log.d(TAG, "后台下载加载数据...");
					//初始化
					final AppContext appContext = (AppContext)getApplicationContext();
					//初始化
					final LessonDao lessonDao = new LessonDao();
					//检查是否从网络下载数据
					if(StringUtils.isNotBlank(classId) && appContext != null && appContext.isNetworkConnected()){
						//初始化参数
						final Map<String, Object> parameters = new HashMap<String, Object>();
						//设置用户ID
						parameters.put("randUserId", AppContext.getCurrentUserId());
						//设置课程ID
						parameters.put("classId", classId);
						//设置是否免费
						parameters.put("free", false);
						
						//请求网络数据
						final JSONCallback<Lesson[]> callback = new APIUtils.CallbackJSON<Lesson[]>(MyCourseLessonActivity.this, Lesson[].class)
								.sendGETRequest(getResources(), R.string.api_lessons_url, parameters);
						if(callback.getSuccess()){
							//删除原有记录
							lessonDao.deleteByClass(classId);
							//新增记录
							lessonDao.add(classId, callback.getData());
						}else{
							this.msg = callback.getMsg();
							Log.e(TAG, "下载课程资源失败:" + callback.getMsg());
						}
					}
					//加载数据库中的数据
					return lessonDao.loadLessonsByClass(classId);
				} catch (Exception e) {
					 Log.e(TAG, "加载数据异常:" + e.getMessage(), e);
				}
				return null;
			}
			/*
			 * 前台主线程更新数据。
			 * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
			 */
			@Override
			protected void onPostExecute(List<Lesson> result) {
				Log.d(TAG, "前台主线程更新数据...");
				if(StringUtils.isNotBlank(this.msg)){
					Toast.makeText(getApplicationContext(), this.msg, Toast.LENGTH_LONG).show();
				}
				//清除数据
				lessons.clear();
				//更新数据
				if(result != null && result.size() > 0){
					Log.d(TAG, "更新数据...");
					lessons.addAll(result);
				}
				//是否显示有无数据
				nodataView.setVisibility(lessons.size() > 0 ? View.GONE : View.VISIBLE);
				//通知数据适配器更新数据
				adapter.notifyDataSetChanged();
			}
		}.execute((Void)null);
	}
	//数据适配器
	private class LessonAdapter extends BaseAdapter{
		private static final String TAG = "LessonAdapter";
		private final List<Lesson> list;
		/**
		 * 构造函数。
		 * @param lessons
		 */
		public LessonAdapter(List<Lesson> lessons){
			Log.d(TAG, "初始化...");
			this.list = lessons;
		}
		/*
		 * 获取数据总数。
		 * @see android.widget.Adapter#getCount()
		 */
		@Override
		public int getCount() {
			return (this.list == null) ? 0 : this.list.size();
		}
		/*
		 * 获取数据对象。
		 * @see android.widget.Adapter#getItem(int)
		 */
		@Override
		public Object getItem(int position) {
			return (this.list == null || this.list.size() < position) ? null : this.list.get(position);
		}
		/*
		 * 获取数据对象ID。
		 * @see android.widget.Adapter#getItemId(int)
		 */
		@Override
		public long getItemId(int position) {
			return position;
		}
		/*
		 * 获取数据项View。
		 * @see android.widget.Adapter#getView(int, android.view.View, android.view.ViewGroup)
		 */
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			Log.d(TAG, "加载数据项View... " + position);
			ViewHolder viewHolder = null;
			if(convertView == null){
				Log.d(TAG, "新建数据项...." + position);
				//加载数据项布局
				convertView = LayoutInflater.from(MyCourseLessonActivity.this).inflate(R.layout.activity_my_course_lesson_item, parent, false);
				//初始化
				viewHolder = new ViewHolder(convertView);
				//缓存
				convertView.setTag(viewHolder);
			}else{
				Log.d(TAG, "重用数据项...." + position);
				 viewHolder = (ViewHolder)convertView.getTag();
			}
			//加载数据
			viewHolder.loadData((Lesson)this.getItem(position));
			//返回
			return convertView;
		}
	}
	//数据项View
	private class ViewHolder implements OnClickListener{
		private static final String TAG = "ViewHolder";
		private TextView tvLesson,tvState;
		private Lesson lesson;
		/**
		 * 构造函数。
		 * @param convertView
		 */
		public ViewHolder(View convertView){
			Log.d(TAG, "初始化...");
			//课程资源名称
			this.tvLesson = (TextView)convertView.findViewById(R.id.txt_lesson);
			//设置点击课程资源名称处理
			this.tvLesson.setOnClickListener(this);
			//下载状态
			this.tvState = (TextView)convertView.findViewById(R.id.tv_state);
			//下载状态按钮
			final View btnDownload = convertView.findViewById(R.id.btn_download);
			//设置点击事件处理
			btnDownload.setOnClickListener(this);
		}
		/**
		 * 加载数据。
		 * @param data
		 */
		public void loadData(Lesson data){
			Log.d(TAG, "加载数据...");
			if(data == null) return;
			//
			this.lesson = data;
			//课程资源名称
			this.tvLesson.setText(this.lesson.getName());
			//下载状态
			if(StringUtils.isNotBlank(this.lesson.getId())){
				//下载是否存在
				if(DownloadFactory.getInstance()
						.hasDownloadFile(new DownloadFactory.DownloadItemConfig(
										this.lesson.getId(),
										this.lesson.getName(),
										this.lesson.getPriorityUrl()))){
					tvState.setText("本地");
					tvState.setTextColor(getResources().getColor(R.color.green));
				}else{
					tvState.setText("在线");
					tvState.setTextColor(getResources().getColor(R.color.grey));
				}
			}
		}
		/*
		 *点击事件处理
		 * @see android.view.View.OnClickListener#onClick(android.view.View)
		 */
		@Override
		public void onClick(View v) {
			Log.d(TAG, "下载点击事件处理...." + v);
			//初始化
			Intent intent = null;
			switch(v.getId()){
				case R.id.txt_lesson:{//点击课程资源名称->播放
					Log.d(TAG, "播放处理...");
					intent = new Intent(MyCourseLessonActivity.this, VideoPlayActivity.class);
					break;
				}
				case R.id.btn_download:{//下载
					Log.d(TAG, "下载处理...");
					intent = new Intent(MyCourseLessonActivity.this, DownloadActivity.class);
					break;
				}
			}
			//发送意图
			if(intent != null){
				//设置课程资源ID
				intent.putExtra(Constant.CONST_LESSON_ID, this.lesson.getId());
				//设置课程资源名称
				intent.putExtra(Constant.CONST_LESSON_NAME, this.lesson.getName());
				//发送意图
				startActivity(intent);
			}
		}
	}
}