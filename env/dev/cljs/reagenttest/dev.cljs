(ns ^:figwheel-no-load reagenttest.dev
  (:require
    [reagenttest.core :as core]
    [devtools.core :as devtools]))


(enable-console-print!)

(devtools/install!)

(core/init!)
