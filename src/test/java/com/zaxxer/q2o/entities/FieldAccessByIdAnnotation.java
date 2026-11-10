package com.zaxxer.q2o.entities;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

/**
 * @author Holger Thurow (thurow.h@gmail.com)
 * @since 10.11.26
 */
//@Entity
//@Table(name = "MyClass")
@MappedSuperclass
public class FieldAccessByIdAnnotation {

    @Id @GeneratedValue
    private int id;
    private String note;
    private String note2;

    @Override
    public String toString() {
        return "IncludeColumns{" +
                "id=" + id +
                ", note='" + note + '\'' +
                ", note2='" + note2 + '\'' +
                '}';
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getNote2() {
        return note2;
    }

    public void setNote2(String note2) {
        this.note2 = note2;
    }
}
