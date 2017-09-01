(ns iwaswhere-electron.update.core
  (:require [iwaswhere-electron.update.log]
            [taoensso.timbre :as timbre :refer-macros [info debug]]
            [iwaswhere-electron.update.ipc :as ipc]
            [iwaswhere-electron.update.ui :as ui]
            [electron :refer [ipcRenderer]]
            [matthiasn.systems-toolbox.switchboard :as sb]))

(defonce switchboard (sb/component :updater/switchboard))


(defn start []
  (info "Starting UPDATER")
  (sb/send-mult-cmd
    switchboard
    [[:cmd/init-comp #{(ipc/cmp-map :updater/ipc-cmp #{:update/check
                                                       :update/check-beta
                                                       :update/download
                                                       :update/install
                                                       :window/close})
                       (ui/cmp-map :updater/ui-cmp)}]

     [:cmd/route {:from :updater/ipc-cmp
                  :to   #{:updater/ui-cmp}}]

     [:cmd/route {:from :updater/ui-cmp
                  :to   #{:updater/ipc-cmp}}]]))


(defn load-handler [ev]
  (start))


(.addEventListener js/window "load" load-handler)
