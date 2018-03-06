(ns reagenttest.core
  (:require
    [reagent.core :as r]
    [clojure.string :as str]
    [taoensso.timbre :as timbre :refer [spy]]
  )
)

(def server_url "http://localhost:3000")
;(def server_url "https://btbtravis-spoken5.herokuapp.com")

(def log (.-log js/console))
; TODO: Move randomColor to backend
(defn randomColor []
  (str "hsl(" (rand-int 360) ", 96%, 55%)")
)
(defn in?
  "true if coll contains elm"
  [coll elm]
  (some #(= elm %) coll))
;var elem_top = $("#my_item").offset()['top'];
;var viewport_height = $(window).height();

; Scroll to the current word is middle of the viewport
(defn center []
  (let [yDis (.-offsetLeft (.-parentElement (.querySelector js/document "p.current.text")))
        width (.-innerWidth js/window)
        el (.querySelector js/document "div.wordstream")]
    (-> (spy (- yDis (/ width 2) ))
        ;(spy {:yDis yDis :width width :el el})
        ;(set! (.-name my-type) "Andy")
        (#(spy (set! (.-scrollLeft el) %)))
        (#(spy [yDis width el])))
  )
)

; Data
(def faketxt "The quick brown fox jumped over the lazy dog. Little did the fox know the dog was laying a trap for him. As soon as the foxed landed a snap of jaws and this rear leg was mangled")
(def updatesAtom (r/atom {})); Atom to hold state of the story init loaded from ajax call then updated over websocket
(def username (r/atom ""))
; Load inital updates from encoded json on data attr somewhere... 
(->
  (.get js/axios (str server_url "/getallupdates")); Make ajax get call to backend to get the big list of updates 
  (#(.then % (fn [x]  
    (->
      (js->clj x :keywordize-keys true); Convert the server responce from json to clj map with usable keys
      ((fn [d] (spy(:data d)))); get the data key
      (reverse)
      ((fn [d] (swap! updatesAtom (fn [old] (identity d))))); Update the updateAtom with its some state
      ;((fn [d] (center)))
      ;TODO: scroll to correct pos
  ))))
)
;; SOCKET.io
(def socket (js/io server_url))
(.on socket "connect" #(println "Connected to socket"))
(.on socket "update" #(spy %))
;socket.emit('chat message', $('#m').val());,c 
;: WATSON 
(defn speek []
  (->
    (.get js/axios (str server_url "/api/speech-to-text/token")); Make ajax get call to backend to get the big list of updates 
    (#(.then % (fn [x]  
      (->
        (js->clj x :keywordize-keys true); Convert the server responce from json to clj map with usable keys
        ((fn [d] (spy(:data d)))); get the data key
        ((fn [token] 
          (let [stream (.recognizeMicrophone (.-SpeechToText js/WatsonSpeech) 
            (js-obj "token" token "objectMode" true "extractResults" true "format" false))]
            (.on stream "data" (fn [data] 
              (.emit socket "word" (clj->js {:words data :user @username}))
            ))
            (.on stream "error" (fn [data] (println data)))
            (js/setTimeout (fn [x] (.stop stream)) 5000)
          )
        ))
    ))))
  )
)
;(speek)
; data processing
(defn txtfragmnets "get text fragments from a string and updates or entries that were maid aginst that string"
  [txt updates]  
  (let [words (->> (str/split txt #"\s"); split txt up by space
               (map #(identity {:word %})); create a map out of these words ex. {:word "hello"}
               (map-indexed (fn [i obj]  (conj obj {:i i}))); insert the index of the word into the map ex. {:word "hello" :i 0}
       )
       currentId (+ 1 (last (:wordIds (last updates))))]
    (println {:currentId currentId :words words})
    (-> updates
      (#(map (fn [a] (conj a {:part (reduce (fn [string word] 
        (if(in? (:wordIds a) (:i word)) (str string " " (:word word)) string)
        ) "" words)})) %))
      (#(map (fn [a] (assoc a :type "past")) %))
      (reverse); idk why the reverse calls are needed
      (#(conj % {:type "current" :part (:word (nth words currentId))})); add the current word
      (#(conj % {:type "future" :part 
        (-> words
          ((fn [a] (filter (fn [word] (> (:i word) currentId)) a)))
          ((fn [a] (reduce (fn [string wordMap] (str string " " (:word wordMap))) "" a)))
        )
      }))
      (reverse); idk why the reverse calls are needed
      ((fn [frags] (map-indexed (fn [i frag] (assoc frag :key i)) frags))) ;set unique keys
      (#(spy %))
      ;((fn [words]; split fragmnets into seprate seq based on u    ;(reverse); idk why the reverse calls are needed
      ;(#(into [] %)); lazy seq -> map
      ;((fn [frags] (map-indexed (fn [i frag] (assoc frag :key i)) frags))) ;set unique keys
    )
  )
)

(println "CLEAR")
(defn user-input []
    [:input {:type "text"
             :value @username
             :on-change #(reset! username (-> % .-target .-value))}])

(defn wordstream 
  []  
  (r/create-class                 ;; <-- expects a map of functions 
    {:component-did-mount               ;; the name of a lifecycle function
      #(println "component-did-mount")   ;; your implementation
     :component-did-update #(center)
     :display-name  "wordstream"  ;; for more helpful warnings & errors
     :reagent-render        ;; Note:  is not :render
         (fn []           ;; remember to repeat parameters
            [:div.wordstream (map #(identity 
              [:div {:key (:key %)} 
                [:p.text {:class (:type %)} (:part %)]
                (when (not= nil (:color %)) [:div.bar {:style {:border-top-color (:color %)}}])
                (when (not= nil (:user %)) [:p.user {:style {:color (:color %)}} (:user %)])
              ]) (txtfragmnets faketxt @updatesAtom ))]
)}))
(defn home-page []
  [:div 
    [:div.btns 
     [:p "Name: " [user-input]]
     [:button { :on-click 
        #(swap! updatesAtom (fn [old]
         (conj old {:user "kelsey 11/01/17" :start 0 :end 5 :color (randomColor) } )
        ))
      } "speak"]
     [:button {:on-click speek} "Speak"]
     [:button {:on-click #(cljs.pprint/pprint @updatesAtom)} "view"]
     [:button {:on-click #(center)} "center"]
    ]
    [wordstream]
    [:p "Username: " @username]
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
