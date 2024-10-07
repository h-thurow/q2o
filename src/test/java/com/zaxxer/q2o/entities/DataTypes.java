package com.zaxxer.q2o.entities;

import jakarta.persistence.*;

import java.math.BigInteger;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;

/**
 * @author Holger Thurow (thurow.h@gmail.com)
 * @since 14.12.19
 */
@Entity
public class DataTypes {
   @Id
   @GeneratedValue(strategy = GenerationType.IDENTITY)
   private int id;

   private int myInteger;

   @Temporal(TemporalType.DATE)
   private Date dateToDate;
   private java.sql.Date sqlDateToDate;
   private Timestamp timestampToDate;

   private Date dateToDateTime;
   private java.sql.Date sqlDateToDateTime;
   private Time timeToDateTime;
   private Timestamp timestampToDateTime;

   /** Must work without @Temporal(value = TemporalType.TIMESTAMP) too. See {@link com.zaxxer.q2o.ValueToFieldTypeConverter#convertTimestamp(String, Class, Object)} */
   @SuppressWarnings("JavadocReference")
   private Date dateToTimestamp;
   private Timestamp timestampToTimestamp;
   private java.sql.Date sqlDateToTimestamp;
   @Temporal(value = TemporalType.TIMESTAMP)
   private Calendar calendarToTimestamp;

   private Integer intToYear;
   private java.sql.Date sqlDateToYear;
   private String stringToYear;

   // YEAR(2): No longer supported by MySQL 8
//      Integer intToYear2;
//      java.sql.Date sqlDateToYear2;
//      String stringToYear2;

   private int intToTime;
   private String stringToTime;
   private Time timeToTime;
   private Timestamp timestampToTime;

   private String stringToChar4;
   private String stringToVarChar4;
   private String stringToBinary;
   private String stringToVarBinary;
   private byte[] byteArrayToBinary;
   private byte[] byteArrayToVarBinary;

   private byte byteToBit8;
   private short shortToBit16;
   private int intToBit32;
   private long longToBit64;
   private String stringToBit8;
   private byte[] byteArrayToBit64;

   private byte byteToTinyint;
   private short shortToTinyint;
   private int intToTinyint;
   private long longToTinyint;

   private byte byteToSmallint;
   private short shortToSmallint;
   private int intToSmallint;
   private long longToSmallint;

   private int intToBigint;
   private long longToBigint;
   private BigInteger bigintToBigint;

   private int intToInt;
   private Integer integerToInt;
   private int intToMediumint;
   private long longToUnsignedInt;

   // CLARIFY Mimic Hibernate? "Hibernate Annotations support out of the box enum type mapping ... the persistence representation, defaulted to ordinal" (Mapping with JPA (Java Persistence Annotations).pdf, "2.2.2.1. Declaring basic property mappings")
   @Enumerated(EnumType.STRING)
   private
   CaseMatched enumToEnumTypeString;

   @Enumerated(EnumType.ORDINAL)
   private
   CaseMatched enumToEnumTypeOrdinal;

   @Enumerated(EnumType.ORDINAL)
   private
   CaseMatched enumToInt;

   public int getId() {
      return id;
   }

   public void setId(int id) {
      this.id = id;
   }

   public int getMyInteger() {
      return myInteger;
   }

   public void setMyInteger(int myInteger) {
      this.myInteger = myInteger;
   }

   public Date getDateToDate() {
      return dateToDate;
   }

   public void setDateToDate(Date dateToDate) {
      this.dateToDate = dateToDate;
   }

   public java.sql.Date getSqlDateToDate() {
      return sqlDateToDate;
   }

   public void setSqlDateToDate(java.sql.Date sqlDateToDate) {
      this.sqlDateToDate = sqlDateToDate;
   }

   public Timestamp getTimestampToDate() {
      return timestampToDate;
   }

   public void setTimestampToDate(Timestamp timestampToDate) {
      this.timestampToDate = timestampToDate;
   }

   public Date getDateToDateTime() {
      return dateToDateTime;
   }

   public void setDateToDateTime(Date dateToDateTime) {
      this.dateToDateTime = dateToDateTime;
   }

   public java.sql.Date getSqlDateToDateTime() {
      return sqlDateToDateTime;
   }

   public void setSqlDateToDateTime(java.sql.Date sqlDateToDateTime) {
      this.sqlDateToDateTime = sqlDateToDateTime;
   }

   public Time getTimeToDateTime() {
      return timeToDateTime;
   }

   public void setTimeToDateTime(Time timeToDateTime) {
      this.timeToDateTime = timeToDateTime;
   }

   public Timestamp getTimestampToDateTime() {
      return timestampToDateTime;
   }

   public void setTimestampToDateTime(Timestamp timestampToDateTime) {
      this.timestampToDateTime = timestampToDateTime;
   }

   public Date getDateToTimestamp() {
      return dateToTimestamp;
   }

   public void setDateToTimestamp(Date dateToTimestamp) {
      this.dateToTimestamp = dateToTimestamp;
   }

   public Timestamp getTimestampToTimestamp() {
      return timestampToTimestamp;
   }

   public void setTimestampToTimestamp(Timestamp timestampToTimestamp) {
      this.timestampToTimestamp = timestampToTimestamp;
   }

   public java.sql.Date getSqlDateToTimestamp() {
      return sqlDateToTimestamp;
   }

   public void setSqlDateToTimestamp(java.sql.Date sqlDateToTimestamp) {
      this.sqlDateToTimestamp = sqlDateToTimestamp;
   }

   public Integer getIntToYear() {
      return intToYear;
   }

   public void setIntToYear(Integer intToYear) {
      this.intToYear = intToYear;
   }

   public java.sql.Date getSqlDateToYear() {
      return sqlDateToYear;
   }

   public void setSqlDateToYear(java.sql.Date sqlDateToYear) {
      this.sqlDateToYear = sqlDateToYear;
   }

   public String getStringToYear() {
      return stringToYear;
   }

   public void setStringToYear(String stringToYear) {
      this.stringToYear = stringToYear;
   }

   public int getIntToTime() {
      return intToTime;
   }

   public void setIntToTime(int intToTime) {
      this.intToTime = intToTime;
   }

   public String getStringToTime() {
      return stringToTime;
   }

   public void setStringToTime(String stringToTime) {
      this.stringToTime = stringToTime;
   }

   public Time getTimeToTime() {
      return timeToTime;
   }

   public void setTimeToTime(Time timeToTime) {
      this.timeToTime = timeToTime;
   }

   public Timestamp getTimestampToTime() {
      return timestampToTime;
   }

   public void setTimestampToTime(Timestamp timestampToTime) {
      this.timestampToTime = timestampToTime;
   }

   public String getStringToChar4() {
      return stringToChar4;
   }

   public void setStringToChar4(String stringToChar4) {
      this.stringToChar4 = stringToChar4;
   }

   public String getStringToVarChar4() {
      return stringToVarChar4;
   }

   public void setStringToVarChar4(String stringToVarChar4) {
      this.stringToVarChar4 = stringToVarChar4;
   }

   public String getStringToBinary() {
      return stringToBinary;
   }

   public void setStringToBinary(String stringToBinary) {
      this.stringToBinary = stringToBinary;
   }

   public String getStringToVarBinary() {
      return stringToVarBinary;
   }

   public void setStringToVarBinary(String stringToVarBinary) {
      this.stringToVarBinary = stringToVarBinary;
   }

   public byte[] getByteArrayToBinary() {
      return byteArrayToBinary;
   }

   public void setByteArrayToBinary(byte[] byteArrayToBinary) {
      this.byteArrayToBinary = byteArrayToBinary;
   }

   public byte[] getByteArrayToVarBinary() {
      return byteArrayToVarBinary;
   }

   public void setByteArrayToVarBinary(byte[] byteArrayToVarBinary) {
      this.byteArrayToVarBinary = byteArrayToVarBinary;
   }

   public byte getByteToBit8() {
      return byteToBit8;
   }

   public void setByteToBit8(byte byteToBit8) {
      this.byteToBit8 = byteToBit8;
   }

   public short getShortToBit16() {
      return shortToBit16;
   }

   public void setShortToBit16(short shortToBit16) {
      this.shortToBit16 = shortToBit16;
   }

   public int getIntToBit32() {
      return intToBit32;
   }

   public void setIntToBit32(int intToBit32) {
      this.intToBit32 = intToBit32;
   }

   public long getLongToBit64() {
      return longToBit64;
   }

   public void setLongToBit64(long longToBit64) {
      this.longToBit64 = longToBit64;
   }

   public String getStringToBit8() {
      return stringToBit8;
   }

   public void setStringToBit8(String stringToBit8) {
      this.stringToBit8 = stringToBit8;
   }

   public byte[] getByteArrayToBit64() {
      return byteArrayToBit64;
   }

   public void setByteArrayToBit64(byte[] byteArrayToBit64) {
      this.byteArrayToBit64 = byteArrayToBit64;
   }

   public byte getByteToTinyint() {
      return byteToTinyint;
   }

   public void setByteToTinyint(byte byteToTinyint) {
      this.byteToTinyint = byteToTinyint;
   }

   public short getShortToTinyint() {
      return shortToTinyint;
   }

   public void setShortToTinyint(short shortToTinyint) {
      this.shortToTinyint = shortToTinyint;
   }

   public int getIntToTinyint() {
      return intToTinyint;
   }

   public void setIntToTinyint(int intToTinyint) {
      this.intToTinyint = intToTinyint;
   }

   public long getLongToTinyint() {
      return longToTinyint;
   }

   public void setLongToTinyint(long longToTinyint) {
      this.longToTinyint = longToTinyint;
   }

   public byte getByteToSmallint() {
      return byteToSmallint;
   }

   public void setByteToSmallint(byte byteToSmallint) {
      this.byteToSmallint = byteToSmallint;
   }

   public short getShortToSmallint() {
      return shortToSmallint;
   }

   public void setShortToSmallint(short shortToSmallint) {
      this.shortToSmallint = shortToSmallint;
   }

   public int getIntToSmallint() {
      return intToSmallint;
   }

   public void setIntToSmallint(int intToSmallint) {
      this.intToSmallint = intToSmallint;
   }

   public long getLongToSmallint() {
      return longToSmallint;
   }

   public void setLongToSmallint(long longToSmallint) {
      this.longToSmallint = longToSmallint;
   }

   public int getIntToBigint() {
      return intToBigint;
   }

   public void setIntToBigint(int intToBigint) {
      this.intToBigint = intToBigint;
   }

   public long getLongToBigint() {
      return longToBigint;
   }

   public void setLongToBigint(long longToBigint) {
      this.longToBigint = longToBigint;
   }

   public BigInteger getBigintToBigint() {
      return bigintToBigint;
   }

   public void setBigintToBigint(BigInteger bigintToBigint) {
      this.bigintToBigint = bigintToBigint;
   }

   public int getIntToInt() {
      return intToInt;
   }

   public void setIntToInt(int intToInt) {
      this.intToInt = intToInt;
   }

   public Integer getIntegerToInt() {
      return integerToInt;
   }

   public void setIntegerToInt(Integer integerToInt) {
      this.integerToInt = integerToInt;
   }

   public int getIntToMediumint() {
      return intToMediumint;
   }

   public void setIntToMediumint(int intToMediumint) {
      this.intToMediumint = intToMediumint;
   }

   public long getLongToUnsignedInt() {
      return longToUnsignedInt;
   }

   public void setLongToUnsignedInt(long longToUnsignedInt) {
      this.longToUnsignedInt = longToUnsignedInt;
   }

   public CaseMatched getEnumToEnumTypeString() {
      return enumToEnumTypeString;
   }

   public void setEnumToEnumTypeString(CaseMatched enumToEnumTypeString) {
      this.enumToEnumTypeString = enumToEnumTypeString;
   }

   public CaseMatched getEnumToEnumTypeOrdinal() {
      return enumToEnumTypeOrdinal;
   }

   public void setEnumToEnumTypeOrdinal(CaseMatched enumToEnumTypeOrdinal) {
      this.enumToEnumTypeOrdinal = enumToEnumTypeOrdinal;
   }

   public CaseMatched getEnumToInt() {
      return enumToInt;
   }

   public void setEnumToInt(CaseMatched enumToInt) {
      this.enumToInt = enumToInt;
   }

   public Calendar getCalendarToTimestamp() {
      return calendarToTimestamp;
   }

   public void setCalendarToTimestamp(Calendar calendarToTimestamp) {
      this.calendarToTimestamp = calendarToTimestamp;
   }

   public enum CaseMatched {
      one, two, three
   }
}
