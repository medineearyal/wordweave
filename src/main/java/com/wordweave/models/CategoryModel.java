package com.wordweave.models;

/**
 * Represents a category to which a blog post can belong.
 * Categories help classify and organize blogs (e.g., Technology, Health, Travel).
 */
public class CategoryModel {
	private int category_id;
	private String name;

	public int getCategory_id() {
		return category_id;
	}
	public void setCategory_id(int category_id) {
		this.category_id = category_id;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
}
