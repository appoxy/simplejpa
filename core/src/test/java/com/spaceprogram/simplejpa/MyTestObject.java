package com.spaceprogram.simplejpa;

import org.apache.commons.lang.builder.ToStringBuilder;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.*;

/**
 * User: treeder
 * Date: Feb 8, 2008
 * Time: 1:08:25 PM
 */
@Entity
public class MyTestObject extends MySuperClass {

    private String name;
    private Integer age;
    private Double income;
    private Date birthday;
    private Double someDouble;
    private Long someLong;
    private BigDecimal someBigDecimal;
    private MyTestObject2 myTestObject2;
    private List<MyTestObject2> myList;
    private String bigString;
    private MyTestObject3 myTestObject3;
    private Collection<String> multiValueProperty;
    private MyEnum myEnumOrdinal;
    private MyEnum myEnumString;

    public MyTestObject() {
    }

    public MyTestObject(String name) {

        this.name = name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public Integer getAge() {
        return age;
    }

    public String toString() {
        /*  return new ReflectionToStringBuilder(this){
            protected boolean accept(Field field) {
                if(field.getType().isArray() || Collection.class.isAssignableFrom(field.getType())){
                    return false;
                }
                return true;
            }
        }.toString();*/
        return new ToStringBuilder(this)
                .append("id", getId())
                .append("name", name)
                .append("income", income)
                .append("age", age)
                .append("birthday", birthday)
                .append("someLong", someLong)
                .append("someDouble", someDouble)
                .toString();
    }

    public Double getIncome() {
        return income;
    }

    public void setIncome(Double income) {
        this.income = income;
    }

    public Date getBirthday() {
        return birthday;
    }

    public void setBirthday(Date birthday) {
        this.birthday = birthday;
    }

    public void setSomeDouble(Double someDouble) {
        this.someDouble = someDouble;
    }

    public Double getSomeDouble() {
        return someDouble;
    }

    public void setSomeLong(Long someLong) {
        this.someLong = someLong;
    }

    public Long getSomeLong() {
        return someLong;
    }

    @ManyToOne
    public MyTestObject2 getMyTestObject2() {
        return myTestObject2;
    }

    public void setMyTestObject2(MyTestObject2 myTestObject2) {
        this.myTestObject2 = myTestObject2;
    }

    @OneToMany(mappedBy = "myTestObject")
    public List<MyTestObject2> getMyList() {
        return myList;
    }

    public void setMyList(List<MyTestObject2> myList) {
        this.myList = myList;
    }

    public void addToMyList(MyTestObject2 myTestObject2) {
        if (myList == null) myList = new ArrayList<MyTestObject2>();
        myList.add(myTestObject2);
    }

    public BigDecimal getSomeBigDecimal() {
        return someBigDecimal;
    }

    public void setSomeBigDecimal(BigDecimal someBigDecimal) {
        this.someBigDecimal = someBigDecimal;
    }

    @Lob
    public String getBigString() {
        return bigString;
    }

    public void setBigString(String bigString) {
        this.bigString = bigString;
    }

    public void setMyTestObject3(MyTestObject3 myTestObject3) {
        this.myTestObject3 = myTestObject3;
    }

    @ManyToOne
    public MyTestObject3 getMyTestObject3() {
        return myTestObject3;
    }

    public Collection<String> getMultiValueProperty() {
        return multiValueProperty;
    }

    public void setMultiValueProperty(Collection<String> multiValueProperty) {
        this.multiValueProperty = multiValueProperty;
    }

    @Enumerated
    public MyEnum getMyEnumOrdinal() {
        return myEnumOrdinal;
    }

    public void setMyEnumOrdinal(MyEnum myEnumOrdinal) {
        this.myEnumOrdinal = myEnumOrdinal;
    }

    @Enumerated(EnumType.STRING)
    public MyEnum getMyEnumString() {
        return myEnumString;
    }

    public void setMyEnumString(MyEnum myEnumString) {
        this.myEnumString = myEnumString;
    }

}
