/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.mahout.text;

import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.lucene.search.TermQuery;
import org.apache.mahout.common.HadoopUtil;
import org.apache.mahout.text.doc.SingleFieldDocument;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class LuceneIndexToSequenceFilesDriverTest extends AbstractLuceneStorageTest {

  private LuceneIndexToSequenceFilesDriver driver;
  private LuceneStorageConfiguration lucene2SeqConf;
  private String idField;
  private List<String> fields;
  private Path seqFilesOutputPath;
  private Configuration conf;

  @Before
  public void before() throws Exception {
    conf = new Configuration();
    conf.set("io.serializations", "org.apache.hadoop.io.serializer.JavaSerialization,"
      + "org.apache.hadoop.io.serializer.WritableSerialization");

    seqFilesOutputPath = new Path("seqfiles");
    idField = "id";
    fields = asList("field");

    driver = new LuceneIndexToSequenceFilesDriver() {
      @Override
      public LuceneStorageConfiguration newLucene2SeqConfiguration(Configuration configuration, List<Path> indexPaths, Path seqPath, String idField, List<String> fields) {
        lucene2SeqConf = new LuceneStorageConfiguration(configuration, indexPaths, seqPath, idField, fields);
        return lucene2SeqConf;
      }
    };

    commitDocuments(new SingleFieldDocument("1", "Mahout is cool"));
    commitDocuments(new SingleFieldDocument("2", "Mahout is cool"));
  }

  @After
  public void after() throws IOException {
    HadoopUtil.delete(conf, seqFilesOutputPath);
    HadoopUtil.delete(conf, getIndexPath());
  }

  @Test
  public void testNewLucene2SeqConfiguration() {
    lucene2SeqConf = driver.newLucene2SeqConfiguration(conf,
      asList(new Path(getIndexPath().toString())),
      seqFilesOutputPath,
      idField,
      fields);

    assertEquals(conf, lucene2SeqConf.getConfiguration());
    assertEquals(asList(getIndexPath()), lucene2SeqConf.getIndexPaths());
    assertEquals(seqFilesOutputPath, lucene2SeqConf.getSequenceFilesOutputPath());
    assertEquals(idField, lucene2SeqConf.getIdField());
    assertEquals(fields, lucene2SeqConf.getFields());
  }

  @Test
  public void testRun() throws Exception {
    String queryField = "queryfield";
    String queryTerm = "queryterm";
    String maxHits = "500";
    String field1 = "field1";
    String field2 = "field2";

    String[] args = new String[]{
      "-d", getIndexPath().toString(),
      "-o", seqFilesOutputPath.toString(),
      "-i", idField,
      "-f", field1 + "," + field2,
      "-q", queryField + ":" + queryTerm,
      "-n", maxHits,
      "-xm", "sequential"
    };

    driver.setConf(conf);
    driver.run(args);

    assertEquals(asList(getIndexPath()), lucene2SeqConf.getIndexPaths());
    assertEquals(seqFilesOutputPath, lucene2SeqConf.getSequenceFilesOutputPath());
    assertEquals(idField, lucene2SeqConf.getIdField());
    assertEquals(asList(field1, field2), lucene2SeqConf.getFields());

    assertTrue(lucene2SeqConf.getQuery() instanceof TermQuery);
    assertEquals(queryField, ((TermQuery) lucene2SeqConf.getQuery()).getTerm().field());
    assertEquals(queryTerm, ((TermQuery) lucene2SeqConf.getQuery()).getTerm().text());
    assertEquals(new Integer(maxHits), (Integer) lucene2SeqConf.getMaxHits());
  }

  @Test
  public void testRun_optionalArguments() throws Exception {
    String[] args = new String[]{
      "-d", getIndexPath().toString(),
      "-o", seqFilesOutputPath.toString(),
      "-i", idField,
      "-f", StringUtils.join(fields, LuceneIndexToSequenceFilesDriver.SEPARATOR_FIELDS)
    };

    driver.setConf(conf);
    driver.run(args);

    assertEquals(asList(getIndexPath()), lucene2SeqConf.getIndexPaths());
    assertEquals(seqFilesOutputPath, lucene2SeqConf.getSequenceFilesOutputPath());
    assertEquals(idField, lucene2SeqConf.getIdField());
    assertEquals(fields, lucene2SeqConf.getFields());
    assertEquals(conf, lucene2SeqConf.getConfiguration());

    assertEquals(LuceneIndexToSequenceFilesDriver.DEFAULT_QUERY, lucene2SeqConf.getQuery());
    assertEquals(LuceneIndexToSequenceFilesDriver.DEFAULT_MAX_HITS, lucene2SeqConf.getMaxHits());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testRun_invalidQuery() throws Exception {
    String[] args = new String[]{
      "-d", getIndexPath().toString(),
      "-o", seqFilesOutputPath.toString(),
      "-i", idField,
      "-f", StringUtils.join(fields, LuceneIndexToSequenceFilesDriver.SEPARATOR_FIELDS),
      "-q", "inva:lid:query"
    };

    driver.setConf(conf);
    driver.run(args);
  }

  @Test
  public void testHelp() throws Exception {
    driver = new LuceneIndexToSequenceFilesDriver();
    driver.run(new String[]{"--help"});
  }
}
