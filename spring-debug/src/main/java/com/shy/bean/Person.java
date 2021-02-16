package com.shy.bean;

public class Person {

	private String name;
	private Integer id;

	public Person() {
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public Person(String name, Integer id) {
		this.name = name;
		this.id = id;
	}

	@Override
	public String toString() {
		return "Person{" +
				"name='" + name + '\'' +
				", id=" + id +
				'}';
	}
}
