(ns iwaswhere-web.ui.entry.briefing.habits
  (:require [matthiasn.systems-toolbox.component :as st]
            [reagent.ratom :refer-macros [reaction]]
            [re-frame.core :refer [subscribe]]
            [iwaswhere-web.utils.misc :as u]
            [iwaswhere-web.utils.parse :as up]
            [clojure.string :as s]
            [iwaswhere-web.ui.entry.utils :as eu]))

(defn habit-sorter
  "Sorts tasks."
  [x y]
  (let [c (compare (get-in x [:habit :priority] :X)
                   (get-in y [:habit :priority] :X))]
    (if (not= c 0) c (compare (get-in y [:habit :points])
                              (get-in x [:habit :points])))))

(defn waiting-habits
  "Renders table with open entries, such as started tasks and open habits."
  [entry local local-cfg put-fn]
  (let [cfg (subscribe [:cfg])
        query-cfg (subscribe [:query-cfg])
        waiting-habits (subscribe [:waiting-habits])
        options (subscribe [:options])
        expand-fn #(swap! local update-in [:expanded-habits] not)
        stories (reaction (:stories @options))
        saga-filter (fn [entry]
                      (if-let [selected (:selected @local)]
                        (let [story (get @stories (:primary-story entry))]
                          (= selected (:linked-saga story)))
                        true))
        entries-map (subscribe [:entries-map])
        habits
        (reaction
          (let [entries-map @entries-map
                find-missing (u/find-missing-entry entries-map put-fn)
                entries (->> @waiting-habits
                             (map (fn [ts] (find-missing ts)))
                             (filter saga-filter)
                             (sort habit-sorter))
                conf (merge @cfg @options)]
            (if (:show-pvt @cfg)
              entries
              (filter (u/pvt-filter conf entries-map) entries))))]
    (fn waiting-habits-list-render [entry local local-cfg put-fn]
      (let [habits (if (:expanded-habits @local) @habits (take 12 @habits))
            tab-group (:tab-group local-cfg)
            today (.format (js/moment.) "YYYY-MM-DD")
            briefing-day (-> entry :briefing :day)]
        (when (and (= today briefing-day) (seq habits))
          [:div
           [:table.habits
            [:tbody
             [:tr {:on-click expand-fn}
              [:th [:span.fa.fa-exclamation-triangle]]
              [:th [:span.fa.fa-diamond]]
              [:th [:span.fa.fa-diamond.penalty]]
              [:th "waiting habit"]]
             (for [entry habits]
               (let [ts (:timestamp entry)
                     text (eu/first-line entry)]
                 ^{:key ts}
                 [:tr {:on-click (up/add-search ts tab-group put-fn)}
                  [:td
                   (when-let [prio (-> entry :habit :priority)]
                     [:span.prio {:class prio} prio])]
                  [:td.award-points
                   (when-let [points (-> entry :habit :points)]
                     points)]
                  [:td.award-points
                   (when-let [penalty (-> entry :habit :penalty)]
                     penalty)]
                  [:td.habit text]]))]]])))))
