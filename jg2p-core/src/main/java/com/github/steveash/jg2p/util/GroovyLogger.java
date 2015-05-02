/*
 * Copyright 2015 Steve Ash
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.steveash.jg2p.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * @author Steve Ash
 */
public class GroovyLogger {
    private static final Logger defaultLog = LoggerFactory.getLogger(GroovyLogger.class);

    private final Logger log;

    public GroovyLogger(Logger log) {
        this.log = log;
    }

    public GroovyLogger() {
        this.log = defaultLog;
    }

    void print(Object o) {
        log.info(Objects.toString(o));
    }

    void println(Object o) {
        log.info(Objects.toString(o));
    }

    void println() {
        log.info("");
    }
}