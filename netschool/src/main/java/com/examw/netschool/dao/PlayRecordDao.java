package com.examw.netschool.dao;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;

import com.examw.netschool.model.PlayRecord;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

/**
 * 播放记录数据操作类。
 * 
 * @author jeasonyoung
 * @since 2015年9月7日
 */
public class PlayRecordDao extends BaseDao {
	private static final String TAG = "PlayRecordDao";
	private SQLiteDatabase db;
	/**
	 * 删除播放记录。
	 * @param recordId
	 */
	public void delete(String [] recordIds){
		Log.d(TAG, "删除播放记录...");
		if(recordIds == null || recordIds.length == 0) return;
		synchronized(dbHelper){
			try {
				final String delSql = "DELETE FROM  tbl_PlayRecords WHERE id in ('"+ StringUtils.join(recordIds, "','") +"') ";
				Log.d(TAG, "delete-sql:" + delSql);
				//初始化
				db = dbHelper.getWritableDatabase();
				//开启事务
				db.beginTransaction();
				//删除数据
				db.execSQL(delSql);
				//设置事务成功
				db.setTransactionSuccessful();
			}catch (Exception e) {
				Log.e(TAG, "删除播放记录异常:" + e.getMessage(), e);
			}finally{
				if(db != null){
					//结束事务
					db.endTransaction();
					//关闭连接
					db.close();
				}
			}
		}
	}
	/**
	 * 删除播放记录。
	 * @param recordId
	 */
	public void delete(String recordId){
		Log.d(TAG, "删除播放记录...");
		if(StringUtils.isBlank(recordId)) return;
		synchronized(dbHelper){
			try {
				final String delSql = "DELETE FROM  tbl_PlayRecords WHERE id = ? ";
				Log.d(TAG, "delete-sql:" + delSql);
				//初始化
				db = dbHelper.getWritableDatabase();
				//开启事务
				db.beginTransaction();
				//删除数据
				db.execSQL(delSql, new Object[]{ recordId });
				//设置事务成功
				db.setTransactionSuccessful();
			}catch (Exception e) {
				Log.e(TAG, "删除播放记录异常:" + e.getMessage(), e);
			}finally{
				if(db != null){
					//结束事务
					db.endTransaction();
					//关闭连接
					db.close();
				}
			}
		}
	}
	/**
	 * 删除课程资源下播放记录。
	 * @param lessonId
	 */
	public void deleteByLesson(String lessonId){
		Log.d(TAG, "删除课程资源["+lessonId+"]下播放记录...");
		if(StringUtils.isBlank(lessonId)) return;
		synchronized(dbHelper){
			try {
				//初始化
				db = dbHelper.getWritableDatabase();
				//开启事务
				db.beginTransaction();
				//删除数据
				db.execSQL("DELETE FROM  tbl_PlayRecords WHERE lesson_id = ? ", new Object[]{ lessonId });
				//设置事务成功
				db.setTransactionSuccessful();
			}catch (Exception e) {
				Log.e(TAG, "删除课程资源["+lessonId+"]下播放记录异常:" + e.getMessage(), e);
			}finally{
				if(db != null){
					//结束事务
					db.endTransaction();
					//关闭连接
					db.close();
				}
			}
		}
	}
	/**
	 * 新增播放记录。
	 * @param data
	 */
	public String add(String lessonId){
		Log.d(TAG, "新增播放记录...");
		String new_record_id = null;
		if(StringUtils.isBlank(lessonId)) return new_record_id;
		synchronized(dbHelper){
			try {
				//初始化
				db = dbHelper.getWritableDatabase();
				//开启事务
				db.beginTransaction();
				//初始化新的播放记录ID
				new_record_id = UUID.randomUUID().toString();
				//新增记录
				db.execSQL("INSERT INTO tbl_PlayRecords(id,lesson_id,playTime) values (?,?,?)", new Object[]{
						new_record_id, lessonId, 0
				});
				//设置事务成功
				db.setTransactionSuccessful();
			}catch (Exception e) {
				Log.e(TAG, "新增播放记录异常:" + e.getMessage(), e);
			}finally{
				if(db != null){
					//结束事务
					db.endTransaction();
					//关闭连接
					db.close();
				}
			}
		}
		return new_record_id;
	}
	/**
	 * 更新播放时间。
	 * @param recordId
	 * @param playTime
	 */
	public void updatePlayTime(String recordId, Integer playTime){
		Log.d(TAG, "准备更新["+recordId+"]播放时间..." + playTime);
		if(StringUtils.isBlank(recordId) || playTime == null) return;
		synchronized(dbHelper){
			//是否存在播放记录
			boolean hasExists = false;
			try {
				//初始化
				db = dbHelper.getWritableDatabase();
				//查询是否存在
				final Cursor cursor = db.rawQuery("SELECT COUNT(0) FROM tbl_PlayRecords WHERE id = ? ", new String[]{ recordId });
				if(cursor.moveToFirst()){
					hasExists = cursor.getInt(0) > 0;
				}
				//关闭查询
				cursor.close();
				//播放记录不存在
				if(hasExists){
					Log.d(TAG, "更新播放记录["+recordId+"]..." + playTime);
					//开启事务
					db.beginTransaction();
					//更新记录
					db.execSQL("UPDATE tbl_PlayRecords SET playTime = ?  WHERE id = ? ", new Object[]{ playTime, recordId});
					//设置事务成功
					db.setTransactionSuccessful();
				}
			}catch (Exception e) {
				Log.e(TAG, "新增播放记录异常:" + e.getMessage(), e);
			}finally{
				if(db != null){
					//结束事务
					if(hasExists) db.endTransaction();
					//关闭连接
					db.close();
				}
			}
		}
	}
	/**
	 * 加载播放记录。
	 * @return
	 */
	public List<PlayRecord> loadPlayRecords(){
		Log.d(TAG, "加载播放记录...");
		final List<PlayRecord> records = new ArrayList<PlayRecord>();
		synchronized(dbHelper){
			try {
				final String query = "SELECT a.id,a.lesson_id,b.name,a.playTime,a.createTime From tbl_PlayRecords a "
						+ " INNER JOIN tbl_Lessones b ON b.id = a.lesson_id ORDER BY a.createTime DESC";
				//
				Log.d(TAG, query);
				//初始化
				db = dbHelper.getReadableDatabase();
				//查询数据
				final Cursor cursor =  db.rawQuery(query, null);
				//循环赋值
				while(cursor.moveToNext()){
					//初始化
					final PlayRecord record = new PlayRecord();
					//播放记录ID
					record.setId(StringUtils.trimToNull(cursor.getString(0)));
					//课程资源ID
					record.setLessonId(StringUtils.trimToNull(cursor.getString(1)));
					//课程资源名称
					record.setLessonName(StringUtils.trimToNull(cursor.getString(2)));
					//播放时间
					record.setPlayTime(cursor.getInt(3));
					//创建时间
					record.setCreateTime(StringUtils.trimToNull(cursor.getString(4)));
					//添加到集合
					records.add(record);
				}
				//关闭
				cursor.close();
			} catch (Exception e) {
				Log.e(TAG, "加载数据异常:" + e.getMessage(), e);
			} finally {
				//关闭连接
				if(db != null) db.close();
			}
		}
		return records;
	}
	/**
	 * 加载播放记录。
	 * @param recordId
	 * @return
	 */
	public PlayRecord getPlayRecord(String recordId){
		Log.d(TAG, "加载播放记录..." + recordId);
		PlayRecord data = null;
		if(StringUtils.isBlank(recordId)) return data;
		synchronized(dbHelper){
			try{
				//sql
				final String query = "SELECT a.id,a.lesson_id,b.name,a.playTime,a.createTime From tbl_PlayRecords a "
						+ " INNER JOIN tbl_Lessones b ON b.id = a.lesson_id WHERE a.id = ?";
				Log.d(TAG, query);
				//初始化
				db = dbHelper.getReadableDatabase();
				//查询数据
				final Cursor cursor =  db.rawQuery(query, new String[]{ StringUtils.trimToEmpty(recordId) });
				//循环赋值
				if(cursor.moveToFirst()){
					//初始化
					data = new PlayRecord();
					//播放记录ID
					data.setId(StringUtils.trimToNull(cursor.getString(0)));
					//课程资源ID
					data.setLessonId(StringUtils.trimToNull(cursor.getString(1)));
					//课程资源名称
					data.setLessonName(StringUtils.trimToNull(cursor.getString(2)));
					//播放时间
					data.setPlayTime(cursor.getInt(3));
					//创建时间
					data.setCreateTime(StringUtils.trimToNull(cursor.getString(4)));
				}
				//关闭
				cursor.close();
			}catch(Exception e){
				Log.e(TAG, "加载数据异常:" + e.getMessage(), e);
			}finally{
				//关闭连接
				if(db != null) db.close();
			}
		}
		return data;
	}
}