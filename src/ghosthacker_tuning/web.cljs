(ns ghosthacker-tuning.web
  "GHOST HACKER: TUNING -- browser host adapter (ADR-2607100900 follow-up
  (b)). Plain reagent, no Web Audio -- TUNING has no real-time beat to
  track (Nei's observational puzzle, not Ren's reflex titles), so a
  button-driven nudge/lock UI is the whole host, same low-complexity
  end of the spectrum as ECHOES's dialogue host.

  Same input/judgment shape as ghosthacker_tuning/terminal.clj: +/- nudges
  the dial by a fixed step, lock commits the current channel's judgment
  via core/lock-in, and a 'static' percentage (proximity, no direction)
  is the only feedback -- rendered to the DOM instead of stdout."
  (:require [reagent.core :as r]
            [reagent.dom :as rdom]
            [ghosthacker-tuning.core :as core]
            [ghosthacker-tuning.logs :as logs]))

(def ^:private step 0.05)

(defn- static-pct
  "distance(dial, target)を0(ぴったり)〜100(遠い)%のstatic表示に変換する
  (terminal.clj/static-pctと同じ計算)。"
  [dial target]
  (js/Math.round (* 100 (min 1.0 (/ (js/Math.abs (- dial target)) 0.5)))))

(defonce state
  (r/atom {:phase :playing        ; :playing | :result
           :channels logs/quiet-static
           :dial core/initial-dial
           :game-state core/initial-state
           :last-judgment nil}))

(defn- nudge! [delta]
  (swap! state update :dial #(core/nudge-dial % delta)))

(defn- lock-in! []
  (let [{:keys [channels dial game-state]} @state
        next-gs (core/lock-in game-state channels dial)
        judgment (last (:judgments next-gs))]
    (swap! state assoc
           :game-state next-gs
           :last-judgment judgment
           :dial core/initial-dial
           :phase (if (core/complete? next-gs channels) :result :playing))))

(defn- restart! []
  (reset! state {:phase :playing
                 :channels logs/quiet-static
                 :dial core/initial-dial
                 :game-state core/initial-state
                 :last-judgment nil}))

(defn- playing-screen []
  (let [{:keys [channels game-state dial last-judgment]} @state
        channel (core/current-channel game-state channels)]
    [:div.tuning-app
     [:h1 "GHOST HACKER: TUNING"]
     [:p.tuning-sub "quiet-static"]
     [:div.tuning-channel (str "-- " (name (:label channel)) " --")]
     [:div.tuning-static (str "static: " (static-pct dial (:target channel)) "%")]
     [:div.tuning-dial (str "dial " (.toFixed dial 2))]
     [:div.tuning-controls
      [:button {:on-click #(nudge! (- step))} "-"]
      [:button.tuning-lock {:on-click lock-in!} "LOCK"]
      [:button {:on-click #(nudge! step)} "+"]]
     (when last-judgment [:div.tuning-judgment (name last-judgment)])
     [:p.tuning-hint (str (:channel-index game-state) "/" (count channels) " channels tuned")]]))

(defn- result-screen []
  (let [summary (core/summary (:game-state @state))]
    [:div.tuning-app
     [:h1 "GHOST HACKER: TUNING"]
     [:h2 (str "grade: " (name (:grade summary)))]
     [:p (str "score " (:score summary) " / max-combo " (:max-combo summary))]
     [:p (str "accuracy " (.toFixed (* 100 (:accuracy summary)) 0) "%")]
     [:button {:on-click restart!} "もう一度"]]))

(defn app []
  (case (:phase @state)
    :result [result-screen]
    [playing-screen]))

(defn ^:export mount []
  (when-let [el (.getElementById js/document "app")]
    (rdom/render [app] el)))

(defn ^:export init [] (mount))
