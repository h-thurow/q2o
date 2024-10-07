package com.zaxxer.q2o;

import jakarta.persistence.Id;
import opix.domain.filetypes.filetypes.WinExtensionEntity;
import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.sansorm.DataSources;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author Holger Thurow (thurow.h@gmail.com)
 * @since 26.05.18
 */
public class OrmBaseTest {

   class NumericalId {
      @Id
      int id;
   }

   @Test
   public void asInClause() {
      List<NumericalId> objs = new ArrayList<>();
      for (int i = 0; i < 10; i++) {
         NumericalId obj = new NumericalId();
         obj.id = i;
         objs.add(obj);
      }
      assertEquals("id IN (0,1,2,3,4,5,6,7,8,9)", OrmBase.idsAsInClause(NumericalId.class, objs));
   }

   class StringId {
      @Id
      String id;
   }

   @Test
   public void asInClauseNonNumericalId() {
      ArrayList<StringId> objs = new ArrayList<>();
      for (int i = 0; i < 2; i++) {
         StringId obj = new StringId();
         obj.id = String.valueOf(i);
         objs.add(obj);
      }
      assertEquals("id IN ('0','1')", OrmBase.idsAsInClause(StringId.class, objs));
   }


   class NumericalCompositeId {
      @Id
      int id1;
      @Id
      int id2;
   }


   @Test
   public void asInClauseCompositeKeys() {
      ArrayList<NumericalCompositeId> objs = new ArrayList<>();
      for (int i = 0; i < 2; i++) {
         NumericalCompositeId obj = new NumericalCompositeId();
         obj.id1 = i;
         obj.id2 = i;
         objs.add(obj);
      }
      assertEquals("( ( id1=0 AND id2=0) OR ( id1=1 AND id2=1))", OrmBase.idsAsInClause(NumericalCompositeId.class, objs));
   }

   class StringCompositeId {
      @Id
      String id1;
      @Id
      String id2;
   }


   @Test
   public void asInClauseNonNumericalCompositeKeys() {
      ArrayList<StringCompositeId> objs = new ArrayList<>();
      for (int i = 0; i < 2; i++) {
         StringCompositeId obj = new StringCompositeId();
         obj.id1 = i + "";
         obj.id2 = i + "";
         objs.add(obj);
      }
      assertEquals("( ( id1='0' AND id2='0') OR ( id1='1' AND id2='1'))", OrmBase.idsAsInClause(StringCompositeId.class, objs));
   }

   class MixedTypeCompositeKey {
      @Id
      String id1;
      @Id
      int id2;
   }

   @Test
   public void asInClauseMixedTypeCompositeKey() {
      ArrayList<MixedTypeCompositeKey> objs = new ArrayList<>();
      for (int i = 0; i < 2; i++) {
         MixedTypeCompositeKey obj = new MixedTypeCompositeKey();
         obj.id1 = i + "";
         obj.id2 = i;
         objs.add(obj);
      }
      assertEquals("( ( id1='0' AND id2=0) OR ( id1='1' AND id2=1))", OrmBase.idsAsInClause(MixedTypeCompositeKey.class, objs));
   }

   @Test
   public void bulkDelete() throws IOException {
      q2o.initializeTxNone(DataSources.getH2ServerDataSource());
      try {
         Q2Sql.executeUpdate("create table D_WIN_EXTENSION\n" +
            "(" +
            "  WE_FTIDENT numeric(8)  not null\n" +
//            "    constraint WE1_CNST\n" +
//            "    references D_FILE_TYPES (FT_IDENT)\n" +
            ", WE_EXTENSION varchar(12) not null\n" +
            "    unique\n" +
            ")");

         File winExtensionDdlFile = new File("src/test/resources/D_WIN_EXTENSION_omcdev.sql");

         List<String> inserts = FileUtils.readLines(winExtensionDdlFile, "UTF8");
         inserts.forEach(insert -> {
            Q2Sql.executeUpdate(insert);
         });
         assertEquals(53, Q2Obj.countFromClause(WinExtensionEntity.class, null));
         List<WinExtensionEntity> exts = Q2ObjList.fromClause(WinExtensionEntity.class, "WE_FTIDENT = 48");
         assertEquals(3, Q2ObjList.delete(exts));
         assertEquals(50, Q2Obj.countFromClause(WinExtensionEntity.class, null));
      }
      finally {
         Q2Sql.executeUpdate("drop table D_WIN_EXTENSION");
         q2o.deinitialize();
      }

   }
}
