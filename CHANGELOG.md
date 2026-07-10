# Changelog

pure `.cljc` 整合パズル核（`ghosthacker-tuning.core`）と、それを使う
プロトタイプ実装の変更履歴（ADR-2607023200 addendum 2）。

## Unreleased

- 初期実装: `core.cljc`（dial-vs-target判定、`:perfect`/`:good`/`:miss`、
  score/combo/grade）、`logs.cljc`（サンプルパズル`quiet-static`、4
  チャンネル）、`terminal.clj`（プレイ可能なnudge&lock REPLプロトタイプ、
  staticのみで目標値を教えない）、`web.cljs`（ブラウザhostアダプタ、
  reagent、ADR-2607100900 follow-up (b)）。headless DOM上で実クリック
  操作による通し（4チャンネルlock→result画面→もう一度で初期状態に復帰）
  を手動検証済み。
