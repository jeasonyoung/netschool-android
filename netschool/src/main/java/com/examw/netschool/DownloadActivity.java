package com.examw.netschool;

import org.apache.commons.lang3.StringUtils;

import com.examw.netschool.app.Constant;
import com.examw.netschool.dao.LessonDao;
import com.examw.netschool.model.Lesson;
import com.examw.netschool.util.DownloadFactory;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * 下载Activity。
 * @author jeasonyoung
 *
 */
public class DownloadActivity extends FragmentActivity implements OnCheckedChangeListener {
	private static final String TAG = "downloadActivity";
	private String lessonId;
	
	private int index = 0;
	private ViewPager viewPager;
	private RadioGroup radioGroup;
	/**
	 * 索引参数键。
	 */
	public static final String CONST_FRAGMENT_INDEX = "page_index";
	/**
	 * 下载课程UI。
	 */
	public static final int CONST_FRAGMENT_DOWNING = 0;
	/**
	 * 离线课程UI。
	 */
	public static final int CONST_FRAGMENT_FINISH = 1;

	/*
	 * 重载创建
	 * @see android.support.v4.app.FragmentActivity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(TAG, "重载创建....");
		//设置布局文件 
		this.setContentView(R.layout.activity_download);
		
		//加载传递数据
		final Intent intent = this.getIntent();
		if(intent != null){
			//课程资源ID
			this.lessonId = intent.getStringExtra(Constant.CONST_LESSON_ID);
			//页面索引
			index = intent.getIntExtra(CONST_FRAGMENT_INDEX, CONST_FRAGMENT_DOWNING);
		}
		
		//返回按钮
		final View btnBack = this.findViewById(R.id.btn_return);
        if(btnBack != null) {
            btnBack.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.d(TAG, "返回事件处理...");
                    finish();
                }
            });
        }
		//顶部标题
		final TextView tvTitle = (TextView)this.findViewById(R.id.top_title);
		if(tvTitle != null){
			tvTitle.setText(R.string.download_top_title);
		}
		
		//分组选项
		this.radioGroup = (RadioGroup)this.findViewById(R.id.down_radio_group);
        if(this.radioGroup != null) {
            radioGroup.setOnCheckedChangeListener(this);
        }
		//ViewPager
		this.viewPager = (ViewPager)this.findViewById(R.id.download_pagers);
        if(this.viewPager != null) {
            //设置数据适配器
            this.viewPager.setAdapter(this.mAdapter);
        }
	}
	/*
	 * 重载启动。
	 * @see android.support.v4.app.FragmentActivity#onStart()
	 */
	@Override
	protected void onStart() {
		super.onStart();
		Log.d(TAG, "重载启动...");
		//异步加载数据
		new AsyncTask<Void, Void, Void>() {
			/*
			 * 后台线程处理。
			 * @see android.os.AsyncTask#doInBackground(java.lang.Object[])
			 */
			@Override
			protected Void doInBackground(Void... params) {
				try{
					Log.d(TAG, "后台线程加载下载数据...");
					//检查课程资源ID
					if(StringUtils.isBlank(lessonId)) return null;
					//查询课程信息
					final Lesson lesson = new LessonDao().getLesson(lessonId);
					if(lesson != null){
						//初始化下载配置
						final List<DownloadFactory.DownloadItemConfig> configs = new ArrayList<>();
						configs.add(new DownloadFactory.DownloadItemConfig(lesson.getId(), lesson.getName(), lesson.getPriorityUrl()));
						//下载工厂实例
						DownloadFactory.getInstance()
								.beginRequest(configs, true);
					}
				}catch(Exception e){
					Log.e(TAG, "后台线程加载下载数据异常:" + e.getMessage(), e);
				}
				return null;
			}
			/*
			 * 前端主线程更新数据。
			 * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
			 */
			@Override
			protected void onPostExecute(Void result) {
				 Log.d(TAG, "前端主线程更新数据...");
				 //通知数据适配器更新数据
				 mAdapter.notifyDataSetChanged();
				 //
				 final int pageIndex = Math.min(index, mAdapter.getCount() - 1);
				 radioGroup.check(pageIndex == CONST_FRAGMENT_DOWNING ?  R.id.btn_downing : R.id.btn_finish);
			}
		}.execute((Void)null);
	}
	/*
	 * 选项卡事件处理。
	 * @see android.widget.RadioGroup.OnCheckedChangeListener#onCheckedChanged(android.widget.RadioGroup, int)
	 */
	@Override
	public void onCheckedChanged(RadioGroup group, int checkedId) {
		Log.d(TAG, "选项卡事件处理..." + checkedId);
		switch(checkedId){
			case R.id.btn_downing:{//下载中
				Log.d(TAG, "下载中...");
				this.viewPager.setCurrentItem(0);
				break;
			}
			case R.id.btn_finish:{//下载完成
				Log.d(TAG, "下载完成...");
				this.viewPager.setCurrentItem(1);
				break;
			}
		}
	}
	//数据适配器。
	private FragmentPagerAdapter mAdapter = new FragmentPagerAdapter(getSupportFragmentManager()) {
		/*
		 * 获取数据总数。
		 * @see android.support.v4.view.PagerAdapter#getCount()
		 */
		@Override
		public int getCount() {
			Log.d(TAG, "加载适配器数据总数...");
			return 2;
		}
		/*
		 * 获取Fragment.
		 * @see android.support.v4.app.FragmentPagerAdapter#getItem(int)
		 */
		@Override
		public Fragment getItem(int pos) {
			Log.d(TAG, "加载Fragment...." + pos);
			Fragment result = null;
			switch(pos){
				 case 0:{//下载中
					 result = new DownloadByFragmentDowning();
					 break;
				 }
				 case 1:{//下载完成
					 result = new DownloadByFragmentFinish();
					 break;
				 }
			}
			return result;
		}
	};
}