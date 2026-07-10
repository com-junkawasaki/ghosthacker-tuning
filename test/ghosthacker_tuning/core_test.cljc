(ns ghosthacker-tuning.core-test
  (:require [clojure.test :refer [deftest is testing]]
            [ghosthacker-tuning.core :as core]))

(deftest nudge-dial-clamps
  (testing "dialは[0.0, 1.0]の範囲にclampされる"
    (is (= 1.0 (core/nudge-dial 0.98 0.1)))
    (is (= 0.0 (core/nudge-dial 0.02 -0.1)))
    (is (= 0.55 (core/nudge-dial 0.5 0.05)))))

(deftest judge-alignment-tiers
  (testing "perfect/good/missの境界"
    (is (= :perfect (core/judge-alignment 0.0)))
    (is (= :perfect (core/judge-alignment core/perfect-window)))
    (is (= :perfect (core/judge-alignment (- core/perfect-window))))
    (is (= :good (core/judge-alignment (+ core/perfect-window 0.001))))
    (is (= :good (core/judge-alignment core/good-window)))
    (is (= :miss (core/judge-alignment (+ core/good-window 0.001))))
    (is (= :miss (core/judge-alignment 0.9)))))

(deftest apply-judgment-state
  (testing ":missはcomboをリセットする"
    (let [after-perfect (core/apply-judgment core/initial-state :perfect)
          after-miss (core/apply-judgment after-perfect :miss)]
      (is (= 1 (:combo after-perfect)))
      (is (= 1000 (:score after-perfect)))
      (is (= 0 (:combo after-miss)))
      (is (= 1000 (:score after-miss)))
      (is (= 1 (:max-combo after-miss)))
      (is (= 2 (:channel-index after-miss)))
      (is (= [:perfect :miss] (:judgments after-miss))))))

(def ^:private channels
  [{:label :a :target 0.5}
   {:label :b :target 0.2}
   {:label :c :target 0.8}])

(deftest current-channel-and-complete
  (testing "channel-indexに応じてcurrent-channel/complete?が正しく動く"
    (is (= {:label :a :target 0.5} (core/current-channel core/initial-state channels)))
    (is (not (core/complete? core/initial-state channels)))
    (let [s3 (assoc core/initial-state :channel-index 3)]
      (is (nil? (core/current-channel s3 channels)))
      (is (core/complete? s3 channels)))))

(deftest lock-in-judges-current-channel
  (testing "lock-inは現在チャンネルのtargetに対して判定し、次へ進む"
    (let [s1 (core/lock-in core/initial-state channels 0.5)]
      (is (= :perfect (last (:judgments s1))))
      (is (= 1 (:channel-index s1)))
      (let [s2 (core/lock-in s1 channels 0.9)] ; target 0.2, way off -> :miss
        (is (= :miss (last (:judgments s2))))
        (is (= 0 (:combo s2)))
        (is (= 2 (:channel-index s2)))))))

(deftest lock-in-no-op-when-complete
  (testing "全チャンネル消化済みならlock-inはstateをそのまま返す"
    (let [done (assoc core/initial-state :channel-index (count channels))]
      (is (= done (core/lock-in done channels 0.5))))))

(deftest accuracy-and-grade
  (testing "accuracyは:missを除いた割合、judgments空なら1.0"
    (is (= 1.0 (core/accuracy core/initial-state)))
    (let [s (-> core/initial-state
                (core/apply-judgment :perfect)
                (core/apply-judgment :good)
                (core/apply-judgment :miss)
                (core/apply-judgment :perfect))]
      (is (= 0.75 (core/accuracy s)))))
  (testing "gradeはaccuracyの閾値で決まる"
    (is (= :perfect-tune (core/grade (assoc core/initial-state :judgments [:perfect :perfect]))))
    (is (= :d (core/grade (assoc core/initial-state :judgments [:miss :miss :perfect]))))))

(deftest play-and-play-summary
  (testing "playは全チャンネル整合(perfect)すると終了する"
    (let [state (core/play channels [0.5 0.2 0.8])]
      (is (core/complete? state channels))
      (is (= [:perfect :perfect :perfect] (:judgments state)))))
  (testing "dialsが尽きても未完走ならそこまでのstateを返す"
    (let [state (core/play channels [0.5])]
      (is (not (core/complete? state channels)))
      (is (= 1 (:channel-index state)))))
  (testing "play-summaryはplay+summaryの合成"
    (let [summary (core/play-summary channels [0.5 0.2 0.8])]
      (is (= :perfect-tune (:grade summary)))
      (is (= 1.0 (:accuracy summary)))
      (is (= 3 (:judgment-count summary))))))
