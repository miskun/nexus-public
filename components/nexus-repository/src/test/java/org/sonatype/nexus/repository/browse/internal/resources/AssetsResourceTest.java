/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
package org.sonatype.nexus.repository.browse.internal.resources;

import java.util.Arrays;

import javax.ws.rs.NotFoundException;

import org.sonatype.nexus.common.entity.DetachedEntityId;
import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.common.entity.EntityMetadata;
import org.sonatype.nexus.repository.browse.BrowseResult;
import org.sonatype.nexus.repository.browse.QueryOptions;
import org.sonatype.nexus.repository.browse.api.AssetXO;
import org.sonatype.nexus.repository.browse.internal.api.RepositoryItemIDXO;
import org.sonatype.nexus.repository.maintenance.MaintenanceService;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.AssetEntityAdapter;
import org.sonatype.nexus.rest.Page;
import org.sonatype.nexus.selector.VariableSource;

import com.orientechnologies.orient.core.id.ORID;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AssetsResourceTest
    extends RepositoryResourceTestSupport
{
  AssetsResource underTest;

  @Mock
  EntityMetadata assetTwoEntityMetadata;

  @Mock
  EntityId assetTwoEntityId;

  @Mock
  BrowseResult<Asset> browseResult;

  @Mock
  AssetEntityAdapter assetEntityAdapter;

  @Mock
  VariableSource variableSource;

  @Mock
  MaintenanceService maintenanceService;

  @Mock
  ORID assetOneORID;

  Asset assetOne;

  Asset assetTwo;

  AssetXO assetOneXO;

  AssetXO assetTwoXO;

  @Before
  public void setUp() throws Exception {
    assetOne = getMockedAsset("nameOne", "asset");
    when(assetOneORID.toString()).thenReturn("assetORID");

    assetTwo = getMockedAsset("assetTwo", "asset-two-continuation");

    assetOneXO = buildAssetXO("asset", "nameOne", "http://localhost:8081/repository/maven-releases/nameOne");
    assetTwoXO = buildAssetXO("assetTwo", "nameTwo", "http://localhost:8081/repository/maven-releases/nameTwo");

    underTest = new AssetsResource(browseService, repositoryManagerRESTAdapter, assetEntityAdapter, maintenanceService);
  }

  AssetXO buildAssetXO(String id, String coordinates, String downloadUrl) {
    AssetXO assetXo = new AssetXO();
    assetXo.setId(id);
    assetXo.setCoordinates(coordinates);
    assetXo.setDownloadUrl(downloadUrl);
    return assetXo;
  }

  @Captor
  ArgumentCaptor<QueryOptions> queryOptionsCaptor;

  @Test
  public void checkPath() {
    assertThat(AssetsResource.RESOURCE_URI, is("/rest/beta/assets"));
  }

  @Test
  public void testGetAssetsFirstPage() throws Exception {
    when(browseResult.getTotal()).thenReturn(10L);
    when(browseResult.getResults()).thenReturn(Arrays.asList(assetOne, assetTwo));

    when(browseService.browseAssets(eq(mavenReleases), queryOptionsCaptor.capture())).thenReturn(browseResult);

    Page<AssetXO> assetXOPage = underTest.getAssets(null, "maven-releases");
    assertThat(assetXOPage.getContinuationToken(), is("asset-two-continuation"));
    assertThat(assetXOPage.getItems(), hasSize(2));
  }

  @Test
  public void testGetAssetsLastPage() throws Exception {
    when(browseResult.getTotal()).thenReturn(2l);
    when(browseResult.getResults()).thenReturn(Arrays.asList(assetOne, assetTwo));

    when(browseService.browseAssets(eq(mavenReleases), queryOptionsCaptor.capture())).thenReturn(browseResult);

    Page<AssetXO> assetXOPage = underTest.getAssets(null, "maven-releases");
    assertThat(assetXOPage.getContinuationToken(), isEmptyOrNullString());
    assertThat(assetXOPage.getItems(), hasSize(2));
  }

  private void validateAssetOne(final AssetXO assetXO) {
    assertThat(assetXO.getId(), notNullValue());
    assertThat(assetXO.getCoordinates(), is("nameOne"));
    assertThat(assetXO.getDownloadUrl(), is("http://localhost:8081/repository/maven-releases/nameOne"));
  }

  @Test
  public void testGetAssetById() {
    RepositoryItemIDXO repositoryItemIDXO = new RepositoryItemIDXO("maven-releases",
        "c0ac2ab6c5e93a4a3909f0830fdadfcd");

    when(assetEntityAdapter.recordIdentity(new DetachedEntityId(repositoryItemIDXO.getId()))).thenReturn(assetOneORID);
    when(browseService.getAssetById(assetOneORID, mavenReleases)).thenReturn(assetOne);

    AssetXO assetXO = underTest.getAssetById(repositoryItemIDXO.getValue());

    validateAssetOne(assetXO);
  }

  @Test
  public void testDeleteAsset() {
    RepositoryItemIDXO repositoryItemIDXO = new RepositoryItemIDXO("maven-releases",
        "c0ac2ab6c5e93a4a3909f0830fdadfcd");

    DetachedEntityId entityId = new DetachedEntityId(repositoryItemIDXO.getId());
    when(assetEntityAdapter.recordIdentity(entityId)).thenReturn(assetOneORID);
    when(browseService.getAssetById(assetOneORID, mavenReleases)).thenReturn(assetOne);

    underTest.deleteAsset(repositoryItemIDXO.getValue());
    verify(maintenanceService).deleteAsset(mavenReleases, assetOne);

  }

  @Test(expected = NotFoundException.class)
  public void testGetAssetById_notFound() {
    RepositoryItemIDXO repositoryItemIDXO = new RepositoryItemIDXO("maven-releases",
        "f10bd0593de3b5e4b377049bcaa80d3e");

    when(assetEntityAdapter.recordIdentity(new DetachedEntityId(repositoryItemIDXO.getId()))).thenReturn(assetOneORID);
    when(browseService.getAssetById(assetOneORID, mavenReleases)).thenReturn(null);

    underTest.getAssetById(repositoryItemIDXO.getValue());
  }
}
