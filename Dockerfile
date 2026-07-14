# Use an official, stable JDK 21 base image (required for modern Gradle, Kotlin, and AGP)
FROM --platform=linux/amd64 eclipse-temurin:21-jdk-jammy

# Set environment variables for Android SDK location and system paths
ENV ANDROID_HOME=/opt/android-sdk
ENV PATH=${PATH}:${ANDROID_HOME}/cmdline-tools/latest/bin:${ANDROID_HOME}/platform-tools:${ANDROID_HOME}/build-tools/34.0.0:/opt/gradle-9.3.1/bin

# Install system utilities required for SDK download and execution
RUN apt-get update && apt-get install -y \
    unzip \
    wget \
    git \
    && rm -rf /var/lib/apt/lists/*

# Download and install Android Command Line Tools (cmdline-tools)
RUN mkdir -p ${ANDROID_HOME}/cmdline-tools && \
    wget -q https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip -O /tmp/cmdline-tools.zip && \
    unzip -q /tmp/cmdline-tools.zip -d /tmp/cmdline-tools-extracted && \
    mv /tmp/cmdline-tools-extracted/cmdline-tools ${ANDROID_HOME}/cmdline-tools/latest && \
    rm -rf /tmp/cmdline-tools.zip /tmp/cmdline-tools-extracted

# Automatically accept all Android SDK licenses
RUN yes | sdkmanager --licenses

# Download essential Android SDK platforms and build-tools matching Project Athena's build configuration (SDK 35/34)
RUN sdkmanager "platform-tools" \
    "build-tools;34.0.0" \
    "platforms;android-35"

# Download and install Gradle 9.3.1
RUN wget -q https://services.gradle.org/distributions/gradle-9.3.1-bin.zip -O /tmp/gradle.zip && \
    unzip -q /tmp/gradle.zip -d /opt && \
    rm /tmp/gradle.zip

# Set the workspace directory inside the container
WORKDIR /workspace

# Run the build tool compilation by default when starting the container
CMD ["gradle", "assembleDebug", "--no-daemon"]
