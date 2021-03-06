package com.examw.netschool.model;

import java.io.Serializable;

import org.apache.commons.lang3.StringUtils;
/**
 * 我的课程数据模型。
 * 
 * @author jeasonyoung
 * @since 2015年9月5日
 */
public class PackageClass implements Serializable,Comparable<PackageClass> {
	private static final long serialVersionUID = 1L;
	public static final String TYPE_PACKAGE = "package", TYPE_CLASS = "class";
	
	private String pid,id,name,type;
	private Integer orderNo;
	
	/**
	 * 获取上级ID。
	 * @return 上级ID。
	 */
	public String getPid() {
		return pid;
	}
	/**
	 * 设置上级ID。
	 * @param pid 
	 *	  上级ID。
	 */
	public void setPid(String pid) {
		this.pid = pid;
	}
	/**
	 * 获取ID。
	 * @return ID。
	 */
	public String getId() {
		return id;
	}
	/**
	 * 设置ID。
	 * @param id 
	 *	  ID。
	 */
	public void setId(String id) {
		this.id = id;
	}
	/**
	 * 获取名称。
	 * @return 名称。
	 */
	public String getName() {
		return name;
	}
	/**
	 * 设置名称。
	 * @param name 
	 *	  名称。
	 */
	public void setName(String name) {
		this.name = name;
	}
	/**
	 * 获取类型。
	 * @return 类型。
	 */
	public String getType() {
		return type;
	}
	/**
	 * 设置类型。
	 * @param type 
	 *	  类型。
	 */
	public void setType(String type) {
		this.type = type;
	}
	
	/**
	 * 是否为班级。
	 * @return
	 */
	public boolean IsClass(){
		return StringUtils.isNotBlank(this.type) && StringUtils.equalsIgnoreCase(this.type, TYPE_CLASS);
	}
	/**
	 * 获取排序号。
	 * @return 排序号。
	 */
	public Integer getOrderNo() {
		return orderNo;
	}
	/**
	 * 设置排序号。
	 * @param orderNo 
	 *	  排序号。
	 */
	public void setOrderNo(Integer orderNo) {
		this.orderNo = orderNo;
	}
	/*
	 * 排序比较。
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(PackageClass o) {
		return this.orderNo - o.orderNo;
	}
}