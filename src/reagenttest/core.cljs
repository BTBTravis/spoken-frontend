(ns reagenttest.core
  (:require
    [reagent.core :as r]
    [clojure.string :as str]
  )
)

;(def socket (io "http://localhost"))
;; Data
(defn randomColor []
  (str "hsl(" (rand-int 360) ", 96%, 55%)")
)
; fake test data
(def faketxt "The quick brown fox jumped over the lazy dog. Little did the fox know the dog was laying a trap for him. As soon as the foxed landed a snap of jaws and this rear leg was mangled")
(def fakeupdates [
  {:user "travis 10/24/17" :start 0 :end 5 :color (randomColor) }
  {:user "jim 11/01/17" :start 6 :end 10 :color (randomColor) }
  {:user "bob 11/01/17" :start 11 :end 15 :color (randomColor) }
  {:user "fred 11/01/17" :start 16 :end 17 :color (randomColor) }
])
(def updatesAtom (r/atom fakeupdates))
; TODO: load inital updates from encoded json on data attr somewhere... 
(def socket (js/io "http://localhost:3000"))
(.on socket "connect" #(println "Connected to socket"))
;var socket = io('http://localhost');
  ;socket.on('connect', function(){});
  ;socket.on('event', function(data){});
  ;socket.on('disconnect', function(){});
; data processing
(defn txtfragmnets; get text fragments from a string and updates or entries that were maid aginst that string
  [txt updates]  
  (->
    (->> (str/split txt #"\s"); split txt up by space
         (map #(identity {:word %})); create a map out of these words ex. {:word "hello"}
         (map-indexed (fn [i obj]  (conj obj {:i i}))); insert the index of the word into the map ex. {:word "hello" :i 0}
         ; bind the correct user to this object or nil ex. {:word "hello" :i 0 :user "travis002"}
         (map #(conj 
          % 
          (reduce (fn [user val] 
            (if (and (>= (:end val) (:i %)) (<= (:start val) (:i %)))
             val 
             user
            )
          )  nil updates)
         ))
    )
    ((fn [words]; split fragmnets into seprate seq based on user  
      (conj
        (reverse (map (fn [x] (reverse (drop (:start x) (take (+ 1 (:end x)) words)))) updates))
        (take 1 (drop (+ (:end (last updates)) 1) words))
        (reverse (lazy-seq (drop (+ (:end (last updates)) 2) words)))
      )
    ))
    (reverse); idk why the reverse calls are needed
    ((fn [x] (map #(assoc (first %) :word (reduce (fn [carry val] (str (:word val) " "  carry)) "" %)) x ))); join words into one long string joinning with space
    ((fn [x] (map #(dissoc % :i) x))); remove the :i key it is no longer needed
    (#(into [] %)); lazy seq -> map
    (#(assoc-in % [(- (count %) 1) :type] "future")); attach future key to last element in vector
    (#(assoc-in % [(- (count %) 2) :type] "current")); attach current key to second to last element in vector
    ((fn [frags] (map-indexed (fn [i frag] (assoc frag :key i)) frags))) ;set unique keys
  )
)

(println "CLEAR")
;(.log js/console "Hello, world!")
;(cljs.pprint/pprint (txtfragmnets faketxt fakeupdates))
;(cljs.pprint/pprint (count (txtfragmnets faketxt fakeupdates)))
;(cljs.pprint/pprint (type (txtfragmnets faketxt fakeupdates)))
;(cljs.pprint/pprint (map #(type %) (txtfragmnets faketxt fakeupdates)))

(defn home-page []
  [:div 
    [:div.btns 
     [:button { :on-click 
        #(swap! updatesAtom (fn [old]
         (conj old {:user "kelsey 11/01/17" :start 18 :end 22 :color (randomColor) } )
        ))
      } "speak"]
     [:button {:on-click #(cljs.pprint/pprint @updatesAtom)} "view"]
    ]
    [:div.wordstream (map #(identity 
      [:div {:key (:key %)} 
        [:p.text {:class (:type %)} (:word %)]
        (when (not= nil (:color %)) [:div.bar {:style {:border-top-color (:color %)}}])
        (when (not= nil (:user %)) [:p.user {:style {:color (:color %)}} (:user %)])

      ]) (txtfragmnets faketxt @updatesAtom ))]
    [:div.progressbar [:p "progressbar"]]
    [:div.rankings [:p "rankings"]]
  ]
)


;; -------------------------
;; Initialize app

(defn mount-root []
  (r/render [home-page] (.getElementById js/document "app")))

(defn init! []
  (mount-root))
