package com.intellij.lang.javascript.flex.projectStructure.options;

import com.intellij.openapi.diagnostic.Logger;
import org.jetbrains.annotations.Nullable;

/**
 * @author ksafonov
 */
public class BCUtils {

  private static LinkageType[] LIB_LINKAGES = {LinkageType.Default, LinkageType.Merged, LinkageType.External};
  private static LinkageType[] FLEX_MOBILE_APP_LINKAGES = {LinkageType.Default};
  private static LinkageType[] FLEX_WEB_OR_DESKTOP_APP_LINKAGES = {LinkageType.Default, LinkageType.Merged, LinkageType.RSL};
  private static LinkageType[] AS_APP_LINKAGES = {LinkageType.Default};

  private static Logger LOG = Logger.getInstance(BCUtils.class);

  public static LinkageType[] getSuitableFrameworkLinkages(BuildConfigurationNature nature) {
    if (nature.outputType == FlexIdeBuildConfiguration.OutputType.Library) {
      return LIB_LINKAGES;
    }
    else if (nature.pureAS) {
      return AS_APP_LINKAGES;
    }
    else {
      return nature.isMobilePlatform() ? FLEX_MOBILE_APP_LINKAGES : FLEX_WEB_OR_DESKTOP_APP_LINKAGES;
    }
  }

  public static LinkageType getDefaultFrameworkLinkage(BuildConfigurationNature nature) {
    if (nature.outputType == FlexIdeBuildConfiguration.OutputType.Library) {
      return LinkageType.External;
    }
    else if (nature.pureAS) {
      return LinkageType.Merged;
    }
    else if (nature.isWebPlatform()) {
      return LinkageType.RSL; // Web Flex App
    }
    else {
      return LinkageType.Merged; // AIR Flex App (Desktop or Mobile)
    }
  }

  /**
   * If <code>LinkageType.Default</code> is returned then use {@link #getDefaultFrameworkLinkage(BuildConfigurationNature)} to get real value.
   *
   * @return <code>null</code> if entry should not be included at all
   */
  @Nullable
  public static LinkageType getSdkEntryLinkageType(final String url,
                                                   final BuildConfigurationNature bcNature,
                                                   final FlexIdeBuildConfiguration.ComponentSet componentSet) {
    if (url.endsWith("/frameworks/libs/air/airglobal.swc")) {
      return bcNature.isWebPlatform() ? null : LinkageType.External;
    }

    if (url.endsWith("/playerglobal.swc") && url.contains("/frameworks/libs/player/")) {
      return bcNature.isWebPlatform() ? LinkageType.External : null;
    }

    final int lastSlashIndex = url.lastIndexOf('/');
    if (lastSlashIndex <= 0 || lastSlashIndex == url.length() - 1) {
      LOG.error("Unexpected Flex SDK root: " + url);
    }
    final String swcName = url.substring(lastSlashIndex + 1);
    final String folderUrl = url.substring(0, lastSlashIndex);

    if (folderUrl.endsWith("/frameworks/libs")) {
      return getLibraryFromLibsFolderLinkage(bcNature, componentSet, swcName);
    }

    if (folderUrl.endsWith("/frameworks/libs/air")) {
      return getAirLibraryLinkage(bcNature, componentSet, swcName);
    }

    if (folderUrl.endsWith("/frameworks/libs/mobile")) {
      return getMobileLibraryLinkage(bcNature, swcName);
    }

    if (folderUrl.endsWith("/frameworks/libs/mx")) {
      return getMxLibraryLinkage(bcNature, componentSet, swcName);
    }

    LOG.error("Unknown Flex SDK root: " + url);
    return LinkageType.Merged;
  }

  @Nullable
  private static LinkageType getLibraryFromLibsFolderLinkage(final BuildConfigurationNature bcNature,
                                                             final FlexIdeBuildConfiguration.ComponentSet componentSet,
                                                             final String swcName) {
    if (swcName.equals("advancedgrids.swc")) {
      return bcNature.isMobilePlatform() || bcNature.pureAS || componentSet == FlexIdeBuildConfiguration.ComponentSet.SparkOnly
             ? null
             : LinkageType.Default;
    }

    if (swcName.equals("authoringsupport.swc")) {
      return LinkageType.Merged;
    }

    if (swcName.equals("charts.swc")) {
      return bcNature.pureAS ? null : LinkageType.Default;
    }

    if (swcName.equals("core.swc")) {
      return bcNature.pureAS ? LinkageType.Merged : null;
    }

    if (swcName.equals("datavisualization.swc")) {
      return bcNature.pureAS ? null : LinkageType.Merged;
    }

    if (swcName.endsWith("flash-integration.swc")) {
      return bcNature.pureAS ? null : LinkageType.Merged;
    }

    if (swcName.equals("flex.swc")) {
      return bcNature.pureAS ? LinkageType.Merged : null;
    }

    if (swcName.endsWith("framework.swc")) {
      return bcNature.pureAS ? null : LinkageType.Default;
    }

    if (swcName.endsWith("osmf.swc")) {
      return LinkageType.Default;
    }

    if (swcName.endsWith("rpc.swc")) {
      return bcNature.pureAS ? null : LinkageType.Default;
    }

    if (swcName.endsWith("spark.swc")) {
      return bcNature.pureAS ? null
                             : (bcNature.isMobilePlatform() || componentSet != FlexIdeBuildConfiguration.ComponentSet.MxOnly)
                               ? LinkageType.Default
                               : null;
    }

    if (swcName.endsWith("spark_dmv.swc")) {
      return !bcNature.pureAS && !bcNature.isMobilePlatform() && componentSet == FlexIdeBuildConfiguration.ComponentSet.SparkAndMx
             ? LinkageType.Default : null;
    }

    if (swcName.endsWith("sparkskins.swc")) {
      return !bcNature.pureAS && !bcNature.isMobilePlatform() && componentSet != FlexIdeBuildConfiguration.ComponentSet.MxOnly
             ? LinkageType.Default : null;
    }

    if (swcName.endsWith("textLayout.swc")) {
      return LinkageType.Default;
    }

    if (swcName.endsWith("utilities.swc")) {
      return LinkageType.Merged;
    }

    if (swcName.equals("automation.swc") ||
        swcName.equals("automation_agent.swc") ||
        swcName.equals("automation_dmv.swc") ||
        swcName.equals("automation_flashflexkit.swc") ||
        swcName.equals("qtp.swc")) {
      // additionally installed on top of Flex SDK 3.x
      return LinkageType.Merged;
    }

    LOG.warn("Unknown SWC in '<Flex SDK>/frameworks/libs' folder: " + swcName);
    return LinkageType.Merged;
  }

  @Nullable
  private static LinkageType getAirLibraryLinkage(final BuildConfigurationNature bcNature,
                                                  final FlexIdeBuildConfiguration.ComponentSet componentSet,
                                                  final String swcName) {
    if (bcNature.isMobilePlatform()) {
      return swcName.equals("servicemonitor.swc") ? LinkageType.Merged : null;
    }

    if (bcNature.isDesktopPlatform()) {
      if (swcName.equals("airframework.swc")) {
        return bcNature.pureAS ? null : LinkageType.Merged;
      }

      if (swcName.equals("airspark.swc")) {
        return bcNature.pureAS || componentSet == FlexIdeBuildConfiguration.ComponentSet.MxOnly ? null : LinkageType.Merged;
      }

      return LinkageType.Merged;
    }

    return null;
  }

  @Nullable
  private static LinkageType getMobileLibraryLinkage(final BuildConfigurationNature bcNature, final String swcName) {
    if (!swcName.equals("mobilecomponents.swc")) {
      LOG.warn("Unknown SWC in '<Flex SDK>/frameworks/libs/mobile' folder: " + swcName);
    }
    return bcNature.pureAS ? null : LinkageType.Merged;
  }

  @Nullable
  private static LinkageType getMxLibraryLinkage(final BuildConfigurationNature bcNature,
                                                 final FlexIdeBuildConfiguration.ComponentSet componentSet,
                                                 final String swcName) {
    if (!swcName.equals("mx.swc")) {
      LOG.warn("Unknown SWC in '<Flex SDK>/frameworks/libs/mx' folder: " + swcName);
    }
    return bcNature.isMobilePlatform() || bcNature.pureAS || componentSet == FlexIdeBuildConfiguration.ComponentSet.SparkOnly
           ? null
           : LinkageType.Default;
  }
}
