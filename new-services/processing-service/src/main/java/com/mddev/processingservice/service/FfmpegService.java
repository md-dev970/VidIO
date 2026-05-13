package com.mddev.processingservice.service;

import com.mddev.processingservice.exception.FfmpegException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class FfmpegService {

    public void generateThumbnail(Path inputPath, Path thumbnailPath) {
        runCommand(List.of(
                "ffmpeg",
                "-y",
                "-i", inputPath.toString(),
                "-ss", "00:00:01",
                "-vframes", "1",
                thumbnailPath.toString()
        ), "FFmpeg thumbnail generation failed");
    }

    public void convertTo720p(Path inputPath, Path outputPath) {
        runCommand(List.of(
                "ffmpeg",
                "-y",
                "-i", inputPath.toString(),
                "-vf", "scale=-2:720",
                outputPath.toString()
        ), "FFmpeg 720p conversion failed");
    }

    public double extractDuration(Path inputPath) {
        String output = runCommand(List.of(
                "ffprobe",
                "-v", "error",
                "-show_entries", "format=duration",
                "-of", "default=nw=1:nk=1",
                inputPath.toString()
        ), "FFprobe duration extraction failed");
        return Double.parseDouble(output.trim());
    }

    private String runCommand(List<String> command, String failureMessage) {
        try {
            Process process = new ProcessBuilder(command)
                    .redirectErrorStream(true)
                    .start();
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new FfmpegException(failureMessage + ": " + output);
            }
            return output;
        } catch (IOException exception) {
            throw new FfmpegException(failureMessage, exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new FfmpegException(failureMessage, exception);
        } catch (NumberFormatException exception) {
            throw new FfmpegException("Could not parse video duration", exception);
        }
    }
}
