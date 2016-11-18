module.exports = function (grunt) {

    grunt.initConfig({
        pkg: grunt.file.readJSON('package.json'),
        concat: {
            options: {
                separator: ''
            },
            youtubemanager: {
                src: [
                    'src/main/javascript/components/youtubemanager/youtubeManagerModule.js',
                    'src/main/javascript/components/youtubemanager/**/*.js'
                ],
                dest: 'src/main/resources/javascript/angular/components/ytm-scripts.js'
            }
        },
        copy: {
            main: {
                files: [
                    {
                        expand: true,
                        cwd: 'src/main/javascript/',
                        src: ['components/**/*.html', 'components/**/*.json'],
                        dest: 'src/main/resources/javascript/angular'
                    }]
            }
        },
        watch: {
            files: ['Gruntfile.js', 'src/main/javascript/**/*.js', 'src/main/javascript/**/*.html'],
            tasks: ['concat', 'copy']
        },
        bower_concat: {
            all: {
                dest: 'src/main/resources/javascript/lib/_ytm.js',
                cssDest: 'src/main/resources/css/lib/_ytm.css',
                exclude: [],
                bowerOptions: {
                    relative: false
                },
                mainFiles: {
                    'moment': ['min/moment-with-locales.js'],
                    'rxjs': ['dist/rx.lite.js'],
                    'font-awesome': ['css/font-awesome.css']
                },
                dependencies: {
                    'underscore': 'jquery',
                    'angular': 'jquery'
                }
            }
        }
    });

    grunt.loadNpmTasks('grunt-contrib-watch');
    grunt.loadNpmTasks('grunt-contrib-concat');
    grunt.loadNpmTasks('grunt-contrib-copy');
    grunt.loadNpmTasks('grunt-bower-concat');
    grunt.registerTask('default', ['concat', 'bower_concat', 'copy']);

};