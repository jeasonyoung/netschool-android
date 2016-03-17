package com.examw.netschool.util;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.os.StatFs;
import android.util.Log;

import com.examw.netschool.app.AppContext;
import com.examw.netschool.codec.binary.Hex;
import com.examw.netschool.codec.digest.DigestUtils;
import com.google.gson.Gson;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.RandomAccessFile;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 下载工厂类。
 * Created by yang yong on 16/3/14.
 */
public final class DownloadFactory {
    private static final String TAG = "downloadFactory";
    private static final int DOWNLOAD_THREAD_MAX = 2;//下载最大线程数。
    private static final String DOWNLOAD_FILES_DIR = "videoFiles";//下载文件存储目录
    private static final String DOWNLOAD_TEMPS_DIR = "videoTemps";//下载文件存储目录
    private static final String DOWNLOAD_CONFIG_SUFFIX = ".json";//下载配置文件后缀
    private static final String DOWNLOAD_TEMP_SUFFIX = ".tmp";//下载配置文件后缀

    private File saveFiles, tempFiles;
    private final List<DownloadItemData> downing;

    private final ExecutorService pools;
    private final List<DownloadActionListener> listeners;
    private static DownloadFactory factory;

    /**
     * 获取下载工厂实例对象。
     * @return 下载对象。
     */
    public static synchronized DownloadFactory getInstance(){
        Log.d(TAG, "getInstance: 获取单列实例...");
        if(factory == null){
            Log.d(TAG, "getInstance: 初始化工厂实例...");
            factory = new DownloadFactory();
        }
        //创建下载存储目录
        factory.createPath(AppContext.getCurrentUsername());
        //返回实例对象。
        return factory;
    }

    /**
     * 构造函数。
     */
    private DownloadFactory(){
        Log.d(TAG, "DownloadFactory: 构造函数");
        //初始化线程池
        this.pools = Executors.newFixedThreadPool(DOWNLOAD_THREAD_MAX);
        //初始化下载事件列表
        this.listeners = Collections.synchronizedList(new LinkedList<DownloadActionListener>());
        //初始化正在下载列表
        this.downing = Collections.synchronizedList(new LinkedList<DownloadItemData>());
    }

    /**
     * 获取下载中列表数据。
     * @return 下载中列表数据。
     */
    public List<DownloadItemData> getDowning() {
        Log.d(TAG, "getDowning: ...");
        //加载下载临时文件目录
        this.loadTempFiles();
        //返回
        return downing;
    }

    /**
     * 添加下载事件监听。
     * @param listener
     * 下载事件监听接口。
     */
    public synchronized DownloadFactory addActionListener(DownloadActionListener listener){
        Log.d(TAG, "addActionListener:" + listener);
        if(listener != null && !this.listeners.contains(listener)){
            this.listeners.add(listener);
        }
        return this;
    }

    /**
     * 移除下载事件监听。
     * @param listener
     * 下载事件监听接口。
     */
    public synchronized void removeActionListener(DownloadActionListener listener){
        Log.d(TAG, "removeActionListener: " + listener);
        if(listener != null && this.listeners.contains(listener)){
            this.listeners.remove(listener);
        }
    }

    /**
     * 获取事件监听处理。
     */
    private final DownloadActionListener actionListener = new DownloadActionListener() {

        @Override
        public synchronized void waiting(DownloadItemData data) {
            if(listeners.size() > 0){
                for(DownloadActionListener l : listeners){
                    if(l == null) continue;
                    try {
                        l.waiting(data);
                    }catch (Exception e){
                        Log.e(TAG, "waiting: 分发事件["+l+"]异常=>" + e, e);
                    }
                }
            }
        }

        @Override
        public synchronized void start(DownloadItemData data) {
            if(listeners.size() > 0){
                for(DownloadActionListener l : listeners){
                    if(l == null) continue;
                    try {
                        l.start(data);
                    }catch (Exception e){
                        Log.e(TAG, "start: 分发事件["+l+"]异常=>" + e, e);
                    }
                }
            }
        }

        @Override
        public synchronized void failedDownload(DownloadItemData data, Exception ex) {
            if(listeners.size() > 0){
                for(DownloadActionListener l : listeners){
                    if(l == null) continue;
                    try {
                        l.failedDownload(data, ex);
                    }catch (Exception e){
                        Log.e(TAG, "failedDownload: 分发事件["+l+"]异常=>" + e, e);
                    }
                }
            }
        }

        @Override
        public synchronized void updateProgress(DownloadItemData data) {
            if(listeners.size() > 0){
                for(DownloadActionListener l : listeners){
                    if(l == null) continue;
                    try {
                        l.updateProgress(data);
                    }catch (Exception e){
                        Log.e(TAG, "updateProgress: 分发事件["+l+"]异常=>" + e, e);
                    }
                }
            }
        }

        @Override
        public synchronized void finishedDownload(DownloadItemData data) {
            if(listeners.size() > 0){
                for(DownloadActionListener l : listeners){
                    if(l == null) continue;
                    try {
                        l.finishedDownload(data);
                    }catch (Exception e){
                        Log.e(TAG, "finishedDownload: 分发事件["+l+"]异常=>" + e, e);
                    }
                }
            }
        }
    };


    /**
     * 创建下载存储根目录。
     * @param currentUserId
     * 当前用户ID。
     */
    private void createPath(String currentUserId){
        Log.d(TAG, "createPath:... ");
        File root = Environment.getExternalStorageDirectory();
        if(root == null || !root.exists()){//如果SD卡不存在，则检查内部存储
            Log.d(TAG, "createPath:未检测到SD卡,将使用内部存储! ");
            root = Environment.getDataDirectory();
        }
        if(StringUtils.isBlank(currentUserId)){
            Log.d(TAG, "createPath: 当前用户ID为空，将使用匿名用户");
            currentUserId = "anonymous";
        }
        final Context context = AppContext.getContext();
        root = new File(root + File.separator + context.getPackageName() +
                File.separator + DigestUtils.md5Hex(currentUserId));
        //下载文件存储目录
        this.saveFiles = new File(root + File.separator + DOWNLOAD_FILES_DIR);
        Log.d(TAG, "createPath: 下载文件目录=>" + this.saveFiles);
        //下载文件临时目录
        this.tempFiles = new File(root + File.separator + DOWNLOAD_TEMPS_DIR);
        Log.d(TAG, "createPath: 下载临时文件目录=>" + this.tempFiles);
    }

    /**
     * 创建下载存储文件名。
     * @param config
     * 下载配置。
     * @return 存储文件名。
     */
    private String createSaveFileName(final DownloadItemConfig config){
        Log.d(TAG, "createSaveFileName:...");
        String source = config.getId() + "|" + config.getName();
        if(source.length() > 60){
            source = source.substring(0, 60);
        }
        return Hex.encodeHexString(source.getBytes(Charset.forName("UTF-8")));
    }

    /**
     * 还原下载存储文件名。
     * @param hex
     * 存储文件名。
     * @return 还原后数组(下载ID,下载名称)
     */
    private DownloadItemConfig recoverSaveFileName(String hex){
        Log.d(TAG, "recoverSaveFileName: hex=>" + hex);
        if(StringUtils.isNotBlank(hex)){
            try {
                final byte[] data = Hex.decodeHex(hex.toCharArray());
                final String result = new String(data, Charset.forName("UTF-8"));
                final String[] array = result.split("\\|");
                if(array.length > 1){
                    return new DownloadItemConfig(array[0], array[1], null);
                }
            }catch (Exception e){
                Log.e(TAG, "recoverSaveFileName: 还原失败=>" + e, e);
            }
        }
        return null;
    }

    /**
     * 创建下载文件。
     * @param config
     * 下载配置。
     * @return 下载文件。
     */
    private File createSaveFile(final DownloadItemConfig config){
        Log.d(TAG, "createSaveFile: " + config);
        if(config != null){
            return new File(this.saveFiles, this.createSaveFileName(config));
        }
        return null;
    }

    /**
     * 发送删除文件广播。
     * @param uri
     * 删除的文件地址。
     */
    private void sendBordCastDelete(Uri uri){
        if(uri != null){
            Context context = AppContext.getContext();
            if(context != null){
                Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                intent.setData(uri);
                context.sendBroadcast(intent);

                Log.d(TAG, "sendBordCastDelete: " + uri);
            }
        }
    }

    /**
     * 删除下载文件。
     * @param config
     * 下载配置。
     */
    private void deleteSaveFile(final DownloadItemConfig config){
        Log.d(TAG, "deleteSaveFile: " + config);
        final File file = this.createSaveFile(config);
        if(file != null && file.exists()){
            Uri uri = Uri.fromFile(file);
            boolean result = file.delete();
            Log.d(TAG, "deleteSaveFile: 删除文件["+file+"]" + result);
            this.sendBordCastDelete(uri);
        }
    }

    /**
     * 加载已下载的数据列表。
     * @return 已下载列表。
     */
    public List<DownloadItemConfig> loadFinishedFiles(){
        Log.d(TAG, "loadFinishedFiles: ...");
        final List<DownloadItemConfig> list = new ArrayList<DownloadItemConfig>();
        //检查文件目录是否存在
        if(this.saveFiles != null && this.saveFiles.exists()){
            //加载目录下全部文件
            final File[] allFiles = this.saveFiles.listFiles();
            if(allFiles != null && allFiles.length > 0){
                for(File file : allFiles){
                    //排除非文件
                    if(!file.isFile()) continue;
                    //临时文件或配置文件排除
                    final String fName = file.getName();
                    if(fName.endsWith(DOWNLOAD_CONFIG_SUFFIX) ||
                            fName.endsWith(DOWNLOAD_TEMP_SUFFIX)) continue;
                    final DownloadItemConfig itemConfig = this.recoverSaveFileName(fName);
                    if(itemConfig != null){
                        //设置文件路径。
                        itemConfig.setUrl(file.getAbsolutePath());
                        //设置文件大小
                        itemConfig.setSize(file.length());
                        //添加到列表
                        list.add(itemConfig);
                    }else{//删除不符合规则的下载文件
                        Uri uri = Uri.fromFile(file);
                        boolean result = file.delete();
                        Log.d(TAG, "loadFinishedFiles: 删除文件=>[" + fName + "]" + result);
                        this.sendBordCastDelete(uri);
                    }
                }
            }
        }
        return list;
    }

    /**
     * 加载下载临时目录。
     */
    private void loadTempFiles(){
        Log.d(TAG, "loadTempFiles: ...");
        if(this.tempFiles != null && this.tempFiles.exists()){
            //加载临时目录下的文件
            final File[] configs = this.tempFiles.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String filename) {
                    return filename.endsWith(DOWNLOAD_CONFIG_SUFFIX);
                }
            });
            //下载配置文件存在
            if(configs != null && configs.length > 0){
                final List<DownloadItemConfig> configList = new ArrayList<DownloadItemConfig>();
                for(File file : configs){
                    //排除不存在或非文件
                    if(!file.exists() || !file.isFile()) continue;
                    //排除非配置文件
                    if(!file.getName().endsWith(DOWNLOAD_CONFIG_SUFFIX)) continue;
                    //加载配置内容
                    final DownloadItemConfig config = this.loadConfigContent(file);
                    if(config != null){
                        configList.add(config);
                        Log.d(TAG, "loadTempFiles: item=>" + config);
                    }
                }
                //是否存在临时文件配置
                if(configList.size() > 0){
                    this.beginRequest(configList, false);
                }
            }
        }
    }

    /**
     * 创建下载配置文件。
     * @param config
     * 配置数据。
     * @return
     * 配置文件。
     */
    private File createConfigFile(final DownloadItemConfig config){
        if(config != null && StringUtils.isNotBlank(config.getId())){
            return new File(this.tempFiles, config.getId() + DOWNLOAD_CONFIG_SUFFIX);
        }
        return null;
    }

    /**
     * 保存下载配置文件。
     * @param config
     * 配置数据。
     */
    private synchronized void saveConfigFile(final DownloadItemConfig config){
        if(config == null) return;
        if(config instanceof DownloadItemData){
            this.saveConfigFile(((DownloadItemData)config).toConfig());
            return;
        }
        Log.d(TAG, "saveConfigFile: " + config);
        final File cfgFile = this.createConfigFile(config);
        if(cfgFile != null){
            try {
                Gson g = new Gson();
                final String json = g.toJson(config);
                Log.d(TAG, "saveConfigFile: json=>" + json);
                if(StringUtils.isNotBlank(json)){
                    FileWriter writer = new FileWriter(cfgFile);
                    writer.write(json);
                    writer.close();
                    Log.d(TAG, "saveConfigFile: 写入文件成功!");
                }
            }catch (Exception e){
                Log.e(TAG, "saveConfigFile: 生成json文件["+cfgFile+"]异常:" + e, e);
            }
        }
    }

    /**
     * 删除下载配置文件。
     * @param config
     * 配置数据。
     */
    private void deleteConfigFile(final DownloadItemConfig config){
        Log.d(TAG, "deleteConfigFile:" + config);
        final File cfgFile = this.createConfigFile(config);
        if(cfgFile != null && cfgFile.exists()){
            Uri uri = Uri.fromFile(cfgFile);
            boolean result = cfgFile.delete();
            Log.d(TAG, "deleteConfigFile:[" + cfgFile + "]=>" + result);
            this.sendBordCastDelete(uri);
        }
    }

    /**
     * 加载配置文件内容。
     * @param configFile
     * 配置文件
     * @return 配置内容。
     */
    private DownloadItemConfig loadConfigContent(File configFile){
        Log.d(TAG, "loadConfigContent: " + configFile);
        if(configFile != null && configFile.canRead()){
            try {
                Gson g = new Gson();
                return g.fromJson(new FileReader(configFile), DownloadItemConfig.class);
            }catch (Exception e){
                Log.e(TAG, "loadConfigContent: 解析JSON文件["+configFile+"]异常=>" + e, e);
            }
        }
        return null;
    }

    /**
     * 创建下载临时文件。
     * @param config
     * 下载配置。
     * @return 临时文件。
     */
    private File createTempFilePath(final DownloadItemConfig config){
        if(config != null && StringUtils.isNotBlank(config.getId())){
            Log.d(TAG, "createTempFilePath: " + config);
            final String tempFileName = config.getId() + DOWNLOAD_TEMP_SUFFIX;
            return new File(this.tempFiles,tempFileName);
        }
        return null;
    }

    /**
     * 删除下载临时文件。
     * @param config
     * 下载配置。
     */
    private void deleteTempFile(final DownloadItemConfig config){
        if(config != null && StringUtils.isNotBlank(config.getId())){
            Log.d(TAG, "deleteTempFile: " + config);
            final File temp = new File(this.tempFiles,config.getId() + DOWNLOAD_TEMP_SUFFIX);
            if(temp.exists()){
                Uri uri = Uri.fromFile(temp);
                boolean result = temp.delete();
                Log.d(TAG, "deleteTempFile: 删除文件["+temp+"]" + result);
                this.sendBordCastDelete(uri);
            }
        }
    }

    /**
     * 开始下载请求。
     * @param configs
     * 下载数据配置。
     * @param isBeginDown
     * 是否开启下载。
     */
    public void beginRequest(final List<DownloadItemConfig> configs, boolean isBeginDown){
        Log.d(TAG, "beginRequest: ...");
        //检查下载目录是否存在
        if(!this.saveFiles.exists()){
            boolean result = this.saveFiles.mkdirs();
            Log.d(TAG, "beginRequest: 创建下载目录["+this.saveFiles+"]" + result);
        }
        //检查下载临时目录是否存在
        if(!this.tempFiles.exists()){
            boolean result = this.tempFiles.mkdirs();
            Log.d(TAG, "beginRequest: 创建下载临时目录["+this.tempFiles+"]" + result);
        }
        //循环下载配置
        if(configs != null && configs.size() > 0){
            int pos = -1;
            for(DownloadItemConfig cfg : configs){
                //判断配置
                if(cfg == null || StringUtils.isBlank(cfg.getId()) ||
                        StringUtils.isBlank(cfg.getUrl())){
                    Log.d(TAG, "beginRequest: 下载ID或URL不存在=>"+ cfg);
                    continue;
                }
                //创建保存配置
                this.saveConfigFile(cfg);
                //
                final DownloadItemData data = new DownloadItemData(cfg);
                //下载临时文件
                final File tempFile = this.createTempFilePath(cfg);
                if(tempFile != null && tempFile.exists()){//临时文件存在
                    //设置已下载数据
                    data.setReceivedSize(tempFile.length());
                }
                //如果文件重复下载或暂停，继续，则把队列中的请求删除，重新添加
                if(this.downing.size() > 0){
                    pos = this.downing.indexOf(data);
                    if(pos > -1) {
                        final DownloadItemData item = this.downing.get(pos);
                        if(item.isDownloading()){
                            Log.d(TAG, "beginRequest: 正在下载中,不可重复下载=>" + item);
                            continue;
                        }
                        //移除
                        this.downing.remove(data);
                    }
                }
                //开始下载
                if(isBeginDown){
                    data.setDownloading(false);
                    data.setWaiting(true);
                    //添加到下载队列
                    this.pools.execute(new DownloadTask(data, tempFile, this.createSaveFile(data)));
                }else{
                    data.setDownloading(false);
                    data.setWaiting(false);
                }
                //添加到下载列表
                if(pos > -1 && this.downing.size() > pos){//暂停恢复
                    this.downing.add(pos, data);
                }else{
                    this.downing.add(data);
                }
                //事件处理
                this.actionListener.waiting(data);
            }
        }
    }

    /**
     * 在下载列表中查找下载数据。
     * @param config
     * 下载配置。
     * @return 下载数据。
     */
    private DownloadItemData findDowning(DownloadItemConfig config){
        if(config != null && StringUtils.isNotBlank(config.getId()) && this.downing.size() > 0){
            for(DownloadItemData data : this.downing){
                if(StringUtils.equals(data.getId(), config.getId())){
                    return data;
                }
            }
        }
        return null;
    }

    /**
     * 取消下载。
     * @param config
     * 下载数据。
     */
    public void cancelDownload(final DownloadItemConfig config){
        if(config == null || StringUtils.isBlank(config.getId())) return;
        if(this.downing.size() > 0){
            Log.d(TAG, "cancelDownload: 取消下载=>" + config);
            final DownloadItemData data = this.findDowning(config);
            if(data != null){
                //获取下载任务
                final DownloadTask task = data.getDownloadTask();
                //取消下载
                if(task != null) task.setCancel(true);
                //删除配置文件
                this.deleteConfigFile(config);
                //删除临时文件
                this.deleteTempFile(config);
                //从下载队列中移除
                this.downing.remove(data);
                //下载中状态取消
                data.setDownloading(false);
                //等待状态取消
                data.setWaiting(false);
                //取消事件处理
                this.actionListener.finishedDownload(data);
            }
        }
    }

    /**
     * 暂停下载。
     * @param config
     * 下载配置。
     */
    public void pauseDownload(final DownloadItemConfig config){
        if(config == null || StringUtils.isBlank(config.getId())) return;
        if(this.downing.size() > 0){
            Log.d(TAG, "pauseDownload: 暂停下载=>" + config);
            final DownloadItemData data = this.findDowning(config);
            if(data != null){
                //获取下载任务
                final DownloadTask task = data.getDownloadTask();
                //取消下载
                if(task != null) task.setCancel(true);
                //下载中状态取消
                data.setDownloading(false);
                //等待状态取消
                data.setWaiting(false);
                //暂停事件处理
                this.actionListener.updateProgress(data);
            }
        }
    }

    /**
     * 恢复继续下载。
     * @param config
     * 下载配置。
     */
    public void continueDownload(final DownloadItemConfig config){
        if(config == null || StringUtils.isBlank(config.getId())) return;
        Log.d(TAG, "continueDownload: 恢复继续下载=>" + config);
        //初始化下载配置
        final List<DownloadItemConfig> configs = new ArrayList<DownloadItemConfig>();
        //添加下载配置
        configs.add(config);
        //开启请求下载。
        this.beginRequest(configs, true);
    }

    /**
     * 删除已下载的数据。
     * @param config
     * 下载数据。
     */
    public void deleteDownload(final DownloadItemConfig config){
        Log.d(TAG, "deleteDownload: 删除已下载文件=>" + config);
        if(config != null){
            //删除已下载文件
            this.deleteSaveFile(config);
            //已删除事件处理
            this.actionListener.finishedDownload(new DownloadItemData(config));
        }
    }

    /**
     * 加载已下载的文件路径。
     * @param config
     * 下载配置。
     * @return 文件路径。
     */
    public File loadDownloadFilePath(final DownloadItemConfig config){
        Log.d(TAG, "loadDownloadFilePath:" + config);
        final File file = this.createSaveFile(config);
        if(file != null && file.exists()){
            return file;
        }
        return null;
    }

    /**
     * 是否已下载。
     * @param config
     * 下载配置。
     * @return 是否已下载。
     */
    public boolean hasDownloadFile(final DownloadItemConfig config){
        Log.d(TAG, "hasDownloadFile:" + config);
        return this.loadDownloadFilePath(config) != null;
    }

    /**
     * 下载任务。
     */
    private class DownloadTask implements Runnable{
        private static final String TAG = "downloadTask";
        private static final int NET_STATE_SUCCESS = 200;
        private static final int NET_STATE_RANGE = 206;
        private static final int BUFFER_SIZE = 1024;

        private final DownloadItemData data;
        private final File downloadTempFile, downloadSaveFile;
        private boolean cancel;
        /**
         * 构造函数。
         * @param data
         * 下载数据。
         */
        public DownloadTask(final DownloadItemData data, final File downloadTempFile, final File downloadSaveFile){
            Log.d(TAG, "DownloadTask: 初始化[temp=>"+downloadTempFile+",save=>"+downloadSaveFile+"]...");
            this.data = data;
            this.data.setDownloadTask(this);
            this.downloadTempFile = downloadTempFile;
            this.downloadSaveFile = downloadSaveFile;
            this.cancel = false;
        }

        /**
         * 设置是否取消下载。
         * @param cancel
         * 是否取消下载。
         */
        public void setCancel(boolean cancel) {
            Log.d(TAG, "setCancel: " + cancel);
            this.cancel = cancel;
        }

        /**
         * 线程执行入口。
         */
        @Override
        public void run() {
            try{
                Log.d(TAG, "run: 线程[" + Thread.currentThread() + "]开始执行...");
                //判断是否取消
                if(this.cancel) return;
                //开始下载
                this.data.setWaiting(false);
                this.data.setDownloading(true);
                //开始下载事件通知。
                actionListener.start(this.data);
                //初始化
                HttpClient client;
                int status;
                //下载大小为空
                if(this.data.getSize() <= 0){//获取下载文件大小
                    client = new DefaultHttpClient();
                    final HttpHead httpHead = new HttpHead(this.data.getUrl());
                    final HttpResponse response = client.execute(httpHead);
                    if((status = response.getStatusLine().getStatusCode()) != NET_STATE_SUCCESS){
                        Log.e(TAG, "run:网络连接["+this.data.getUrl()+"]状态=>" + status);
                        throw new Exception("网络连接失败[" + status + "]!");
                    }
                    final Header[] headers = response.getHeaders("Content-Length");
                    if(headers != null && headers.length > 0){
                        httpHead.abort();
                        this.data.setSize(Long.parseLong(headers[0].getValue()));
                        Log.d(TAG, "run: 获取文件["+this.data+"]大小=>" + this.data.getSize());
                        //重新保存配置
                        saveConfigFile(this.data.toConfig());
                    }else{
                        httpHead.abort();
                        Log.e(TAG, "run:无法获取下载文件["+this.data+"]大小! ");
                        throw new Exception("获取下载文件大小失败!");
                    }
                }
                //已下载的数据
                long pos = this.data.getReceivedSize();
                //判断容量是否可以下载
                if(!this.hasSpace(this.downloadTempFile, this.data.getSize() - pos)){
                    throw new Exception("剩余空间不足,无法完成下载!");
                }
                if(pos > 0){
                    //检查是否支持断点续传
                    final HttpHead httpHead = new HttpHead(this.data.getUrl());
                    httpHead.addHeader("Range", "bytes=0-" + (this.data.getSize() - 1));
                    client = new DefaultHttpClient();
                    final HttpResponse response = client.execute(httpHead);
                    status = response.getStatusLine().getStatusCode();
                    if(status != NET_STATE_RANGE && status != NET_STATE_SUCCESS) {
                        Log.d(TAG, "run: 请求地址["+this.data.getUrl()+"]不支持断点续传=>" + status);
                        //删除已下载的临时文件
                        deleteTempFile(this.data);
                        //重置位置
                        pos = 0;
                    }
                    httpHead.abort();
                }
                //准备下载数据 请求GET
                final HttpGet httpGet = new HttpGet(this.data.getUrl());
                if(pos > 0) {//设置断点续传
                    //当前线程下载起止点.
                    httpGet.addHeader("Range", "bytes=" + pos + "-" + (this.data.getSize() - 1));
                }
                //初始化
                client = new DefaultHttpClient();
                //执行请求
                final HttpResponse response = client.execute(httpGet);
                //获取状态
                status = response.getStatusLine().getStatusCode();
                if(status != NET_STATE_SUCCESS && status != NET_STATE_RANGE){
                    Log.d(TAG, "run: 下载["+this.data.getUrl()+"]文件失败=>" + status);
                    throw new Exception("下载文件失败:" + status);
                }
                //更新进度
                actionListener.updateProgress(this.data);
                //读取请求返回数据
                final HttpEntity entity = response.getEntity();
                //流缓冲
                final BufferedInputStream inputStream = new BufferedInputStream(entity.getContent());
                //存储文件随机读取操作(用于分段大文件处理)
                final RandomAccessFile randomAccessFile = new RandomAccessFile(this.downloadTempFile, "rwd");
                //设置开始写文件位置
                randomAccessFile.seek(pos);
                //读取缓存
                byte[] buf = new byte[BUFFER_SIZE];
                //开始循环读取
                long total = pos, size = this.data.getSize();
                int len, o_per = (int)(((float)total/(float)size) * 1000);
                //下载数据
                while(!this.cancel && (len = inputStream.read(buf, 0, buf.length)) > 0){
                    //写入保存文件
                    randomAccessFile.write(buf, 0, len);
                    //累计下载量
                    total += len;
                    //更新已下载数据
                    this.data.setReceivedSize(total);
                    //更新进度
                    final int n_per = (int)(((float)total/(float)size) * 1000);
                    if(n_per > o_per) {
                        o_per = n_per;
                        actionListener.updateProgress(this.data);
                    }
                    //
                    Log.d(TAG, "run: [" + Thread.currentThread().getName() + "]下载=>" + total+"["+len+"](" + n_per + ")");
                }
                //关闭随机文件写入
                randomAccessFile.close();
                //下载完成
                this.data.setDownloading(false);
                this.data.setWaiting(false);
                //是否为取消
                if(!this.cancel) {
                    //删除下载配置
                    deleteConfigFile(this.data);
                    //从正在下载中移除
                    if (downing.contains(this.data)) {
                        Log.d(TAG, "run: 从下载列表中移除=>" + this.data);
                        downing.remove(this.data);
                    }
                    //移动文件
                    if (this.downloadSaveFile.exists()) {//目标文件存在，则删除目标文件
                        boolean result = this.downloadSaveFile.delete();
                        Log.d(TAG, "run: 删除目标文件[" + this.downloadSaveFile + "]" + result);
                    }
                    //移动文件
                    if (this.downloadTempFile.exists()) {
                        boolean result = this.downloadTempFile.renameTo(this.downloadSaveFile);
                        Log.d(TAG, "run: 移动临时文件到存储文件[" + this.downloadTempFile + "=>" + this.downloadSaveFile + "]" + result);
                    }
                    //下载完成事件处理
                    actionListener.finishedDownload(this.data);
                }
            }catch(Exception e){
                this.data.setWaiting(false);
                this.data.setDownloading(false);
                actionListener.failedDownload(this.data,e);
                Log.e(TAG, "run: 线程执行异常=>" + e, e);
            }finally {
                //删除配置中的下载线程
                this.data.setDownloadTask(null);
                Log.d(TAG, "run: 线程[" + Thread.currentThread() + "]执行完毕!");
            }
        }

        /**
         * 判断是否有空间存储。
         * @param file
         * 文件位置。
         * @param max
         * 需要最大空间。
         * @return 是否用空间存储。
         */
        private boolean hasSpace(final File file,final long max){
            if(file != null) {
                if(!file.exists()){
                    return hasSpace(file.getParentFile(), max);
                }
                try {
                    final StatFs statFs = new StatFs(file.getPath());
                    final long space = (long) (statFs.getAvailableBlocks() * statFs.getBlockSize());
                    if (space < max) {
                        Log.d(TAG, "hasSpace: 路径[" + file + "]下剩余空间[" + space + "<" + max + "]不足!");
                        return false;
                    }
                }catch (Exception e){
                    Log.e(TAG, "hasSpace:["+ file +"]=>" + e.getMessage(), e);
                }
            }
            return true;
        }
    }

    /**
     * 下载项配置。
     */
    public static class DownloadItemConfig implements Serializable {
        private String id,name,url;
        private Long size;

        /**
         * 构造函数。
         * @param id
         * 下载ID。
         * @param name
         * 下载名称。
         * @param url
         * 下载URL。
         * @param size
         * 下载大小。
         */
        public DownloadItemConfig(String id,String name,String url,Long size){
            this.setId(id);
            this.setName(name);
            this.setUrl(url);
            this.setSize(size);
        }

        /**
         * 构造函数。
         * @param id
         * 下载ID。
         * @param name
         * 下载名称。
         * @param url
         * 下载URL。
         */
        public DownloadItemConfig(String id,String name,String url){
            this(id, name, url, 0L);
        }

        /**
         * 构造函数。
         */
        public DownloadItemConfig(){
            this(null,null,null);
        }

        /**
         * 获取下载ID。
         * @return 下载ID。
         */
        public String getId() {
            return id;
        }

        /**
         * 设置下载ID。
         * @param id
         * 下载ID。
         */
        public void setId(String id) {
            this.id = id;
        }

        /**
         * 获取下载名称。
         * @return 下载名称。
         */
        public String getName() {
            return name;
        }

        /**
         * 设置下载名称。
         * @param name
         * 下载名称。
         */
        public void setName(String name) {
            this.name = name;
        }

        /**
         * 获取下载URL。
         * @return 下载URL。
         */
        public String getUrl() {
            return url;
        }

        /**
         * 设置下载URL。
         * @param url
         * 下载URL。
         */
        public void setUrl(String url) {
            this.url = url;
        }

        /**
         * 获取下载大小。
         * @return 下载大小。
         */
        public Long getSize() {
            return size;
        }

        /**
         * 设置下载大小。
         * @param size
         * 下载大小。
         */
        public void setSize(Long size) {
            this.size = size;
        }

        /**
         * 重载。
         * @return
         * toString
         */
        @Override
        public String toString() {
            return StringUtils.join(new String[]{
                    this.getId(),
                    this.getName(),
                    this.getUrl()
            },",");
        }

        /**
         * 重载比较结果。
         * @param o
         * 比较对象。
         * @return 比较结果。
         */
        @Override
        public boolean equals(Object o) {
            if(o != null && (o instanceof DownloadItemConfig)){
                return StringUtils.equals(this.getId(),((DownloadItemConfig)o).getId());
            }
            return super.equals(o);
        }
    }

    /**
     * 下载项数据。
     */
    public static class DownloadItemData extends DownloadItemConfig{
        private boolean downloading,waiting;
        private Long receivedSize;
        private DownloadTask downloadTask;
        /**
         * 构造函数。
         * @param config
         * 下载配置
         */
        public DownloadItemData(DownloadItemConfig config){
            if(config != null){
                this.setId(config.getId());
                this.setName(config.getName());
                this.setUrl(config.getUrl());
                this.setSize(config.getSize());
            }
            this.receivedSize = 0L;
        }

        /**
         * 获取是否下载中。
         * @return 是否下载中。
         */
        public boolean isDownloading() {
            return downloading;
        }

        /**
         * 设置是否下载中。
         * @param downloading
         * 是否下载中。
         */
        public void setDownloading(boolean downloading) {
            this.downloading = downloading;
        }

        /**
         * 获取是否下载等待。
         * @return 是否下载等待。
         */
        public boolean isWaiting() {
            return waiting;
        }

        /**
         * 设置是否下载等待。
         * @param waiting
         * 是否下载等待。
         */
        public void setWaiting(boolean waiting) {
            this.waiting = waiting;
        }

        /**
         * 获取接收数据。
         * @return 接收数据。
         */
        public Long getReceivedSize() {
            return receivedSize;
        }

        /**
         * 设置接收数据。
         * @param receivedSize
         * 接收数据。
         */
        public void setReceivedSize(Long receivedSize) {
            this.receivedSize = receivedSize;
        }

        /**
         * 获取下载任务。
         * @return 下载任务。
         */
        DownloadTask getDownloadTask() {
            return downloadTask;
        }

        /**
         * 设置下载任务。
         * @param downloadTask
         * 下载任务。
         */
        void setDownloadTask(DownloadTask downloadTask) {
            this.downloadTask = downloadTask;
        }

        /**
         * 重载比较。
         * @param o
         * 比较对象。
         * @return 比较结果。
         */
        @Override
        public boolean equals(Object o) {
            if(o != null && (o instanceof DownloadItemData)){
                return StringUtils.equals(this.getId(), ((DownloadItemData)o).getId());
            }
            return super.equals(o);
        }

        /**
         * 转换为配置对象。
         * @return 配置对象。
         */
        DownloadItemConfig toConfig(){
            DownloadItemConfig config = new DownloadItemConfig();
            config.setId(this.getId());
            config.setName(this.getName());
            config.setUrl(this.getUrl());
            config.setSize(this.getSize());
            return config;
        }
    }

    /**
     * 下载事件监听器。
     */
    public interface DownloadActionListener{
        /**
         * 下载等待。
         * @param data
         * 下载数据。
         */
        void waiting(final DownloadItemData data);

        /**
         * 开始下载。
         * @param data
         * 下载数据。
         */
        void start(final DownloadItemData data);

        /**
         * 下载失败。
         * @param data
         * 下载数据。
         * @param e
         * 失败异常。
         */
        void failedDownload(final DownloadItemData data,final Exception e);

        /**
         * 更新下载进度。
         * @param data
         * 下载数据。
         */
        void updateProgress(final DownloadItemData data);

        /**
         * 完成下载数据。
         * @param data
         * 下载数据。
         */
        void finishedDownload(final DownloadItemData data);
    }
}
