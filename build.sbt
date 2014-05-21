import sbtunidoc.Plugin.UnidocKeys.unidoc

scalacOptions in (ScalaUnidoc, sbtunidoc.Plugin.UnidocKeys.unidoc) ++=
  Opts.doc.sourceUrl("https://github.com/choffmeister/secloud/blob/master€{FILE_PATH}.scala")
