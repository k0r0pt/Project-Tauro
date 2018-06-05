/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.koreops.tauro.cli.dao;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.koreops.tauro.core.db.DbConnEngine;
import org.koreops.tauro.core.exceptions.DbDriverException;
import org.koreops.tauro.core.loggers.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest(UpdaterDao.class)
public class UpdaterDaotest {

  @Mock
  DbConnEngine dbConnEngine;
  @Mock
  Connection mockConn;
  @Mock
  Statement mockPreparedStmnt;
  @Mock
  ResultSet mockResultSet;


  @Before
  public void init() {
    PowerMockito.mockStatic(DbConnEngine.class);
    when(dbConnEngine.getConnection()).thenReturn(mockConn);
    when(dbConnEngine.getConnection(anyString(), anyString())).thenReturn(mockConn);
    doNothing().when(mockConn).commit();
    when(mockConn.prepareStatement(anyString(), anyInt())).thenReturn(mockPreparedStmnt);
    doNothing().when(mockPreparedStmnt).setString(anyInt(), anyString());
    when(mockPreparedStmnt.execute()).thenReturn(Boolean.TRUE);
    when(mockPreparedStmnt.getGeneratedKeys()).thenReturn(mockResultSet);
    when(mockResultSet.next()).thenReturn(Boolean.TRUE, Boolean.FALSE);
  }

  @Test
  public void test_saveStation() throws Exception {
    UpdaterDao subject = new UpdaterDao();
    Map<String, String> wifiData = new HashMap;
    wifiData.put("BSSID","1");
    wifiData.put("SSID","2");
    wifiData.put("AuthType","AuthType");
    subject.saveStation(, null);
  }

}