(ns ghosthacker-tuning.logs
  "GHOST HACKER: TUNING -- sample channel set (pure data, ADR-2607023200
  addendum 2).

  \"quiet-static\": Neiが放課後の教室で気づいた、4つの噛み合わないログ
  (教室WiFi・スマホの基地局ping・監視カメラのタイムスタンプ・ルータの
  内部クロック)を順に整合させる短い1本。target値そのものはNeiの観察眼
  (host adapter側の『static』表示)を通じてしか分からない -- ここには
  ロジック用の生の数値として置いてあるだけで、プレイヤーには見せない
  想定。"
  (:require [ghosthacker-tuning.core :as core]))

(def quiet-static
  [{:label :classroom-wifi :target 0.42}
   {:label :phone-tower-ping :target 0.71}
   {:label :cctv-timestamp :target 0.18}
   {:label :router-clock :target 0.63}])

(defn play-quiet-static
  "quiet-staticをdialsで再生し、summaryを返す(core/play-summaryの薄い
  ラッパー)。"
  [dials]
  (core/play-summary quiet-static dials))
