(ns reagenttest.core
    (:require
      [reagent.core :as r]))

;; -------------------------
;; Views
(def wordstream [
  {:user "travis 10/24/17" :str "the quick brown fox" :color "#1EFAD2" :type "past"}
  {:user "kelsey 11/01/17" :str "jumped over the lazy dog." :color "#FA1EBC" :type "past"}
  {:str "Just" :type "current"}
  {:str "then someting bad happen. Oh no said James WFT is going on here anyway?" :type "future"}
])

(defn home-page []
  [:div 
    [:div.btns [:button "speak"][:button "view"]]
    ;[:div.wordstream (map (fn [x] ([:p x])) [1 2 3 4])] 
    [:div.wordstream (map #(identity [:div
      [:p.text 
       {:class (:type %)}
       (:str %)
      ]
      (when (= "past" (:type %)) [:div.bar {:style {:border-top-color (:color %)}}])
      (when (= "past" (:type %)) [:p.user {:style {:color (:color %)}} (:user %)])
      
     ]) wordstream)] 
    [:div.progressbar [:p "progressbar"]]
    [:div.rankings [:p "rankings"]]
  ]
  ;[:div [:h2 "Welcome to Hell"]]
  ;[:div [:h2 "Test"]]
  
  )

;; -------------------------
;; Initialize app

(defn mount-root []
  (r/render [home-page] (.getElementById js/document "app")))

(defn init! []
  (mount-root))
