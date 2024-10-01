package com.zaxxer.q2o.entities;

import jakarta.persistence.*;
import java.math.BigInteger;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;

/**
 * @author Holger Thurow (thurow.h@gmail.com)
 * @since 20.12.19
 */
@Entity
@Table(name = "datatypes")
public class DataTypesNullable {
   @Id
   @GeneratedValue(strategy = GenerationType.IDENTITY)
   private
   int id;

   private Integer myInteger;

   @Temporal(TemporalType.DATE)
   private Date utilDateToDATE;
   private java.sql.Date sqlDateToDATE;
   private Timestamp timestampToDATE;
   @Temporal(value = TemporalType.DATE)
   private Calendar calendarToDATE;

   private Date dateToDATETIME;
   private java.sql.Date sqlDateToDATETIME;
   private Time timeToDATETIME;
   private Timestamp timestampToDATETIME;

   @Temporal(TemporalType.TIMESTAMP)
   private Date utilDateToTIMESTAMP;
   private Timestamp timestampToTIMESTAMP;
   private java.sql.Date sqlDateToTIMESTAMP;
   @Temporal(value = TemporalType.TIMESTAMP)
   private Calendar calendarToTIMESTAMP;

   private Integer intToYEAR;
   private java.sql.Date sqlDateToYEAR;
   private String stringToYEAR;

   // YEAR(2): No longer supported by MySQL 8
//      Integer intToYear2;
//      java.sql.Date sqlDateToYear2;
//      String stringToYear2;

   private Integer intToTIME;
   private String stringToTIME;
   private Time timeToTIME;
   private Timestamp timestampToTIME;
   @Temporal(value = TemporalType.TIME)
   private Calendar calendarToTIME;
   @Temporal(TemporalType.TIME)
   private Date utilDateToTIME;

   private String stringToCHAR4;
   private String stringToVarCHAR4;
   private String stringToBINARY;
   private String stringToVARBINARY;
   private byte[] byteArrayToBINARY;
   private byte[] byteArrayToVARBINARY;

   private Byte byteToBIT8;
   private Short shortToBIT16;
   private Integer intToBIT32;
   private Long longToBIT64;
   private String stringToBIT8;
   private byte[] byteArrayToBIT64;

   private Byte byteToTINYINT;
   private Short shortToTINYINT;
   private Integer intToTINYINT;
   private Long longToTINYINT;

   private Byte byteToSMALLINT;
   private Short shortToSMALLINT;
   private Integer intToSMALLINT;
   private Long longToSMALLINT;

   private Integer intToBIGINT;
   private Long longToBIGINT;
   private BigInteger bigintToBIGINT;

   private Integer intToINT;
   private Integer intToMEDIUMINT;
   private Long longToINT_UNSIGNED;

   // CLARIFY Mimic Hibernate? "Hibernate Annotations support out of the box enum type mapping ... the persistence representation, defaulted to ordinal" (Mapping with JPA (Java Persistence Annotations).pdf, "2.2.2.1. Declaring basic property mappings")
   @Enumerated(EnumType.STRING)
   private CaseMatched enumToENUMString;

   @Enumerated(EnumType.ORDINAL)
   private CaseMatched enumToENUMOrdinal;

   @Enumerated(EnumType.ORDINAL)
   private CaseMatched enumToINTOrdinal;

   @Enumerated(EnumType.STRING)
   private CaseMatched enumToVARCHARString;

   public int getId() {
      return id;
   }

   public void setId(int id) {
      this.id = id;
   }

   public Integer getMyInteger() {
      return myInteger;
   }

   public void setMyInteger(Integer myInteger) {
      this.myInteger = myInteger;
   }

   public Date getUtilDateToDATE() {
      return utilDateToDATE;
   }

   public void setUtilDateToDATE(Date utilDateToDATE) {
      this.utilDateToDATE = utilDateToDATE;
   }

   public java.sql.Date getSqlDateToDATE() {
      return sqlDateToDATE;
   }

   public void setSqlDateToDATE(java.sql.Date sqlDateToDATE) {
      this.sqlDateToDATE = sqlDateToDATE;
   }

   public Timestamp getTimestampToDATE() {
      return timestampToDATE;
   }

   public void setTimestampToDATE(Timestamp timestampToDATE) {
      this.timestampToDATE = timestampToDATE;
   }

   public Date getDateToDATETIME() {
      return dateToDATETIME;
   }

   public void setDateToDATETIME(Date dateToDATETIME) {
      this.dateToDATETIME = dateToDATETIME;
   }

   public java.sql.Date getSqlDateToDATETIME() {
      return sqlDateToDATETIME;
   }

   public void setSqlDateToDATETIME(java.sql.Date sqlDateToDATETIME) {
      this.sqlDateToDATETIME = sqlDateToDATETIME;
   }

   public Time getTimeToDATETIME() {
      return timeToDATETIME;
   }

   public void setTimeToDATETIME(Time timeToDATETIME) {
      this.timeToDATETIME = timeToDATETIME;
   }

   public Timestamp getTimestampToDATETIME() {
      return timestampToDATETIME;
   }

   public void setTimestampToDATETIME(Timestamp timestampToDATETIME) {
      this.timestampToDATETIME = timestampToDATETIME;
   }

   public Date getUtilDateToTIMESTAMP() {
      return utilDateToTIMESTAMP;
   }

   public void setUtilDateToTIMESTAMP(Date utilDateToTIMESTAMP) {
      this.utilDateToTIMESTAMP = utilDateToTIMESTAMP;
   }

   public Timestamp getTimestampToTIMESTAMP() {
      return timestampToTIMESTAMP;
   }

   public void setTimestampToTIMESTAMP(Timestamp timestampToTIMESTAMP) {
      this.timestampToTIMESTAMP = timestampToTIMESTAMP;
   }

   public java.sql.Date getSqlDateToTIMESTAMP() {
      return sqlDateToTIMESTAMP;
   }

   public void setSqlDateToTIMESTAMP(java.sql.Date sqlDateToTIMESTAMP) {
      this.sqlDateToTIMESTAMP = sqlDateToTIMESTAMP;
   }

   public Integer getIntToYEAR() {
      return intToYEAR;
   }

   public void setIntToYEAR(Integer intToYEAR) {
      this.intToYEAR = intToYEAR;
   }

   public java.sql.Date getSqlDateToYEAR() {
      return sqlDateToYEAR;
   }

   public void setSqlDateToYEAR(java.sql.Date sqlDateToYEAR) {
      this.sqlDateToYEAR = sqlDateToYEAR;
   }

   public String getStringToYEAR() {
      return stringToYEAR;
   }

   public void setStringToYEAR(String stringToYEAR) {
      this.stringToYEAR = stringToYEAR;
   }

   public Integer getIntToTIME() {
      return intToTIME;
   }

   public void setIntToTIME(Integer intToTIME) {
      this.intToTIME = intToTIME;
   }

   public String getStringToTIME() {
      return stringToTIME;
   }

   public void setStringToTIME(String stringToTIME) {
      this.stringToTIME = stringToTIME;
   }

   public Time getTimeToTIME() {
      return timeToTIME;
   }

   public void setTimeToTIME(Time timeToTIME) {
      this.timeToTIME = timeToTIME;
   }

   public Timestamp getTimestampToTIME() {
      return timestampToTIME;
   }

   public void setTimestampToTIME(Timestamp timestampToTIME) {
      this.timestampToTIME = timestampToTIME;
   }

   public String getStringToCHAR4() {
      return stringToCHAR4;
   }

   public void setStringToCHAR4(String stringToCHAR4) {
      this.stringToCHAR4 = stringToCHAR4;
   }

   public String getStringToVarCHAR4() {
      return stringToVarCHAR4;
   }

   public void setStringToVarCHAR4(String stringToVarCHAR4) {
      this.stringToVarCHAR4 = stringToVarCHAR4;
   }

   public String getStringToBINARY() {
      return stringToBINARY;
   }

   public void setStringToBINARY(String stringToBINARY) {
      this.stringToBINARY = stringToBINARY;
   }

   public String getStringToVARBINARY() {
      return stringToVARBINARY;
   }

   public void setStringToVARBINARY(String stringToVARBINARY) {
      this.stringToVARBINARY = stringToVARBINARY;
   }

   public byte[] getByteArrayToBINARY() {
      return byteArrayToBINARY;
   }

   public void setByteArrayToBINARY(byte[] byteArrayToBINARY) {
      this.byteArrayToBINARY = byteArrayToBINARY;
   }

   public byte[] getByteArrayToVARBINARY() {
      return byteArrayToVARBINARY;
   }

   public void setByteArrayToVARBINARY(byte[] byteArrayToVARBINARY) {
      this.byteArrayToVARBINARY = byteArrayToVARBINARY;
   }

   public Byte getByteToBIT8() {
      return byteToBIT8;
   }

   public void setByteToBIT8(Byte byteToBIT8) {
      this.byteToBIT8 = byteToBIT8;
   }

   public Short getShortToBIT16() {
      return shortToBIT16;
   }

   public void setShortToBIT16(Short shortToBIT16) {
      this.shortToBIT16 = shortToBIT16;
   }

   public Integer getIntToBIT32() {
      return intToBIT32;
   }

   public void setIntToBIT32(Integer intToBIT32) {
      this.intToBIT32 = intToBIT32;
   }

   public Long getLongToBIT64() {
      return longToBIT64;
   }

   public void setLongToBIT64(Long longToBIT64) {
      this.longToBIT64 = longToBIT64;
   }

   public String getStringToBIT8() {
      return stringToBIT8;
   }

   public void setStringToBIT8(String stringToBIT8) {
      this.stringToBIT8 = stringToBIT8;
   }

   public byte[] getByteArrayToBIT64() {
      return byteArrayToBIT64;
   }

   public void setByteArrayToBIT64(byte[] byteArrayToBIT64) {
      this.byteArrayToBIT64 = byteArrayToBIT64;
   }

   public Byte getByteToTINYINT() {
      return byteToTINYINT;
   }

   public void setByteToTINYINT(Byte byteToTINYINT) {
      this.byteToTINYINT = byteToTINYINT;
   }

   public Short getShortToTINYINT() {
      return shortToTINYINT;
   }

   public void setShortToTINYINT(Short shortToTINYINT) {
      this.shortToTINYINT = shortToTINYINT;
   }

   public Integer getIntToTINYINT() {
      return intToTINYINT;
   }

   public void setIntToTINYINT(Integer intToTINYINT) {
      this.intToTINYINT = intToTINYINT;
   }

   public Long getLongToTINYINT() {
      return longToTINYINT;
   }

   public void setLongToTINYINT(Long longToTINYINT) {
      this.longToTINYINT = longToTINYINT;
   }

   public Byte getByteToSMALLINT() {
      return byteToSMALLINT;
   }

   public void setByteToSMALLINT(Byte byteToSMALLINT) {
      this.byteToSMALLINT = byteToSMALLINT;
   }

   public Short getShortToSMALLINT() {
      return shortToSMALLINT;
   }

   public void setShortToSMALLINT(Short shortToSMALLINT) {
      this.shortToSMALLINT = shortToSMALLINT;
   }

   public Integer getIntToSMALLINT() {
      return intToSMALLINT;
   }

   public void setIntToSMALLINT(Integer intToSMALLINT) {
      this.intToSMALLINT = intToSMALLINT;
   }

   public Long getLongToSMALLINT() {
      return longToSMALLINT;
   }

   public void setLongToSMALLINT(Long longToSMALLINT) {
      this.longToSMALLINT = longToSMALLINT;
   }

   public Integer getIntToBIGINT() {
      return intToBIGINT;
   }

   public void setIntToBIGINT(Integer intToBIGINT) {
      this.intToBIGINT = intToBIGINT;
   }

   public Long getLongToBIGINT() {
      return longToBIGINT;
   }

   public void setLongToBIGINT(Long longToBIGINT) {
      this.longToBIGINT = longToBIGINT;
   }

   public BigInteger getBigintToBIGINT() {
      return bigintToBIGINT;
   }

   public void setBigintToBIGINT(BigInteger bigintToBIGINT) {
      this.bigintToBIGINT = bigintToBIGINT;
   }

   public Integer getIntToINT() {
      return intToINT;
   }

   public void setIntToINT(Integer intToINT) {
      this.intToINT = intToINT;
   }

   public Integer getIntToMEDIUMINT() {
      return intToMEDIUMINT;
   }

   public void setIntToMEDIUMINT(Integer intToMEDIUMINT) {
      this.intToMEDIUMINT = intToMEDIUMINT;
   }

   public Long getLongToINT_UNSIGNED() {
      return longToINT_UNSIGNED;
   }

   public void setLongToINT_UNSIGNED(Long longToINT_UNSIGNED) {
      this.longToINT_UNSIGNED = longToINT_UNSIGNED;
   }

   public CaseMatched getEnumToENUMString() {
      return enumToENUMString;
   }

   public void setEnumToENUMString(CaseMatched enumToENUMString) {
      this.enumToENUMString = enumToENUMString;
   }

   public CaseMatched getEnumToENUMOrdinal() {
      return enumToENUMOrdinal;
   }

   public void setEnumToENUMOrdinal(CaseMatched enumToENUMOrdinal) {
      this.enumToENUMOrdinal = enumToENUMOrdinal;
   }

   public CaseMatched getEnumToINTOrdinal() {
      return enumToINTOrdinal;
   }

   public void setEnumToINTOrdinal(CaseMatched enumToINTOrdinal) {
      this.enumToINTOrdinal = enumToINTOrdinal;
   }

   public Calendar getCalendarToTIMESTAMP() {
      return calendarToTIMESTAMP;
   }

   public void setCalendarToTIMESTAMP(Calendar calendarToTIMESTAMP) {
      this.calendarToTIMESTAMP = calendarToTIMESTAMP;
   }

   public Calendar getCalendarToDATE() {
      return calendarToDATE;
   }

   public void setCalendarToDATE(Calendar calendarToDATE) {
      this.calendarToDATE = calendarToDATE;
   }

   public Calendar getCalendarToTIME() {
      return calendarToTIME;
   }

   public void setCalendarToTIME(Calendar calendarToTIME) {
      this.calendarToTIME = calendarToTIME;
   }

   public Date getUtilDateToTIME() {
      return utilDateToTIME;
   }

   public void setUtilDateToTIME(Date utilDateToTIME) {
      this.utilDateToTIME = utilDateToTIME;
   }

   public CaseMatched getEnumToVARCHARString() {
      return enumToVARCHARString;
   }

   public void setEnumToVARCHARString(CaseMatched enumToVARCHARString) {
      this.enumToVARCHARString = enumToVARCHARString;
   }

   public enum CaseMatched {
      one, two, three
   }
}
