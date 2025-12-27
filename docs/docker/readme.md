# startup.jfr 분석용 Docker 가이드

이 레포지토리에서 JVM Flight Recorder 파일(`startup.jfr`)을 분석할 때 JVM이 설치되지 않은 환경이라면 Docker로 Temurin JDK 21 이미지를 사용하면 됩니다. 아래 단계는 macOS/Linux 기준이지만, Windows PowerShell에서도 거의 동일하게 동작합니다.

## 사전 조건
- Docker Desktop 또는 호환되는 Docker 엔진이 설치되어 있어야 합니다.
- `startup.jfr` 파일이 저장된 프로젝트 루트에서 명령을 실행한다고 가정합니다.

## 요약 정보 확인 (`jfr summary`)
```bash
docker run --rm \  
  -v "$(pwd):/work" \  
  -w /work \  
  eclipse-temurin:21-jdk \  
  jfr summary startup.jfr
```

## 세부 이벤트 출력 (`jfr print`)
```bash
docker run --rm \  
  -v "$(pwd):/work" \  
  -w /work \  
  eclipse-temurin:21-jdk \  
  jfr print --events CPULoad,ClassLoadingStatistics,Compilation --stackdepth 64 startup.jfr
```

## 로컬 실행 팁
1. 필요하다면 `startup.jfr` 경로를 절대경로로 바꿔도 됩니다.
2. 출력이 길 경우 `> output.txt`로 파일에 저장한 뒤 편하게 살펴보세요.
3. 분석 후 불필요한 컨테이너나 이미지가 남지 않도록 `--rm` 옵션이 이미 포함되어 있습니다.
