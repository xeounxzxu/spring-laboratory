# Performance Summary

- Generated at: 2026-03-07 04:52:57 UTC
- k6 results dir: `load/k6/results/rerun-20260307-2`
- wrk results dir: `load/wrk/results/rerun-20260307-2`

| Tool | Endpoint | sleepMs | Case | p95 | p99 | RPS |
| --- | --- | ---: | --- | ---: | ---: | ---: |
| k6 | blocking | 10 | tomcat-max200/rate120 | 14.16ms | 14.44ms | 119.828533/s |
| k6 | blocking | 10 | tomcat-max200/rate200 | 13.09ms | 13.2ms | 199.57381/s |
| k6 | blocking | 100 | tomcat-max200/rate120 | 106.31ms | 106.55ms | 117.118571/s |
| k6 | blocking | 100 | tomcat-max200/rate200 | 105.47ms | 105.58ms | 195.071439/s |
| k6 | io-blocking | 10 | tomcat-max200/rate120 | 14.53ms | 28.51ms | 119.808905/s |
| k6 | io-blocking | 10 | tomcat-max200/rate200 | 13.57ms | 13.71ms | 199.546399/s |
| k6 | io-blocking | 100 | tomcat-max200/rate120 | 106.72ms | 107.15ms | 117.242658/s |
| k6 | io-blocking | 100 | tomcat-max200/rate200 | 105.89ms | 105.98ms | 195.056808/s |
| k6 | io-suspend | 10 | tomcat-max200/rate120 | 29.6ms | 180.28ms | 119.754981/s |
| k6 | io-suspend | 10 | tomcat-max200/rate200 | 14.14ms | 14.4ms | 199.529509/s |
| k6 | io-suspend | 100 | tomcat-max200/rate120 | 107.36ms | 107.78ms | 117.094905/s |
| k6 | io-suspend | 100 | tomcat-max200/rate200 | 106.32ms | 106.49ms | 195.02817/s |
| k6 | suspend | 10 | tomcat-max200/rate120 | 13.76ms | 26.27ms | 119.832115/s |
| k6 | suspend | 10 | tomcat-max200/rate200 | 11.85ms | 12.03ms | 199.630447/s |
| k6 | suspend | 100 | tomcat-max200/rate120 | 103.15ms | 103.41ms | 117.21606/s |
| k6 | suspend | 100 | tomcat-max200/rate200 | 101.81ms | 102.01ms | 195.26484/s |
| k6 | blocking | 10 | tomcat-max50/rate120 | 14.28ms | 14.48ms | 119.868339/s |
| k6 | blocking | 10 | tomcat-max50/rate200 | 13.11ms | 13.16ms | 199.576628/s |
| k6 | blocking | 100 | tomcat-max50/rate120 | 106.28ms | 106.45ms | 117.216776/s |
| k6 | blocking | 100 | tomcat-max50/rate200 | 105.5ms | 105.61ms | 195.090633/s |
| k6 | io-blocking | 10 | tomcat-max50/rate120 | 14.64ms | 32.88ms | 119.823817/s |
| k6 | io-blocking | 10 | tomcat-max50/rate200 | 13.64ms | 14.41ms | 199.645722/s |
| k6 | io-blocking | 100 | tomcat-max50/rate120 | 106.65ms | 106.99ms | 117.194305/s |
| k6 | io-blocking | 100 | tomcat-max50/rate200 | 105.91ms | 106.34ms | 195.113253/s |
| k6 | io-suspend | 10 | tomcat-max50/rate120 | 44.36ms | 196.25ms | 119.78675/s |
| k6 | io-suspend | 10 | tomcat-max50/rate200 | 14.24ms | 14.42ms | 199.519955/s |
| k6 | io-suspend | 100 | tomcat-max50/rate120 | 107.38ms | 107.57ms | 117.196427/s |
| k6 | io-suspend | 100 | tomcat-max50/rate200 | 106.34ms | 106.47ms | 195.034424/s |
| k6 | suspend | 10 | tomcat-max50/rate120 | 13.55ms | 21.06ms | 119.836603/s |
| k6 | suspend | 10 | tomcat-max50/rate200 | 11.92ms | 12.02ms | 199.630895/s |
| k6 | suspend | 100 | tomcat-max50/rate120 | 103.36ms | 103.55ms | 117.198545/s |
| k6 | suspend | 100 | tomcat-max50/rate200 | 101.84ms | 101.97ms | 195.239281/s |
| wrk | blocking | 10 | tomcat-max200/conn120-thr8 | N/A | 14.99ms | 9232.33 |
| wrk | blocking | 100 | tomcat-max200/conn120-thr8 | N/A | 108.26ms | 1130.56 |
| wrk | io-blocking | 10 | tomcat-max200/conn120-thr8 | N/A | 21.53ms | 7332.17 |
| wrk | io-blocking | 100 | tomcat-max200/conn120-thr8 | N/A | 210.44ms | 872.57 |
| wrk | io-suspend | 10 | tomcat-max200/conn120-thr8 | N/A | 20.69ms | 8199.42 |
| wrk | io-suspend | 100 | tomcat-max200/conn120-thr8 | N/A | 112.12ms | 1128.62 |
| wrk | suspend | 10 | tomcat-max200/conn120-thr8 | N/A | 17.29ms | 8731.98 |
| wrk | suspend | 100 | tomcat-max200/conn120-thr8 | N/A | 107.28ms | 1129.14 |
| wrk | blocking | 10 | tomcat-max50/conn120-thr8 | N/A | 37.05ms | 4098.85 |
| wrk | blocking | 100 | tomcat-max50/conn120-thr8 | N/A | 313.18ms | 471.02 |
| wrk | io-blocking | 10 | tomcat-max50/conn120-thr8 | N/A | 15.82ms | 1.24 |
| wrk | io-blocking | 100 | tomcat-max50/conn120-thr8 | N/A | 0.00us | 0.00 |
| wrk | io-suspend | 10 | tomcat-max50/conn120-thr8 | N/A | 0.00us | 0.00 |
| wrk | io-suspend | 100 | tomcat-max50/conn120-thr8 | N/A | 0.00us | 0.00 |
| wrk | suspend | 10 | tomcat-max50/conn120-thr8 | N/A | 97.70ms | 8587.49 |
| wrk | suspend | 100 | tomcat-max50/conn120-thr8 | N/A | 107.42ms | 1129.73 |

## Notes

- wrk 기본 `--latency` 출력은 p50/p75/p90/p99는 제공하지만 p95는 제공하지 않습니다.
- 따라서 별도 퍼센타일 확장 구성이 없으면 wrk 행의 `p95`는 `N/A`로 표시됩니다.
