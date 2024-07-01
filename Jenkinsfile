pipeline {
    agent any
    environment {
        // App Settings
        app_name="PetClinic-Jenkins" //DTP Project

        // Parasoft Licenses
        ls_url="${PARASOFT_LS_URL}" //https\://dtp:8443
        ls_user="${PARASOFT_LS_USER}" //admin
        ls_pass="${PARASOFT_LS_PASS}"
        
        // Parasoft Common Settings
        dtp_url="${PARASOFT_DTP_URL}" //https://dtp:8443
        dtp_user="${PARASOFT_DTP_USER}" //admin
        dtp_pass="${PARASOFT_DTP_PASS}"
        dtp_publish="${PARASOFT_DTP_PUBLISH}" //false
        buildId="${app_name}-${BUILD_TIMESTAMP}"

    }
        stage('Deploy-CodeCoverage') {
            steps {
                // downlaod the agent.jar and cov-tool
                // unzip
                // copy in to the coverage folder
                sh '''
                    cp docker/coverage/*.jar spring-petclinic-customers-service/src/test/resources/coverage/
                    cp docker/coverage/*.jar spring-petclinic-vets-service/src/test/resources/coverage/
                    cp docker/coverage/*.jar spring-petclinic-visits-service/src/test/resources/coverage/
                    '''
                // check running containers
                sh '''
                    docker-compose -f docker-compose-cc.yml down || true
                    '''
                // deploy the project
                sh  '''
                    # Run PetClinic with Jtest coverage agent configured
                    docker-compose -f docker-compose-cc.yml up -d
                    '''

                // Health check coverage agents
                sh '''
                    
                    '''

                // update CTP with yaml script upload
                sh '''
                    # Set Up and write .properties file
                    # TODO

                    # upload yaml file to CTP
                    # TODO
                    '''
            }
        }
                
   post {
        // Clean after build
        always {
            //sh 'docker container stop ${app_name}'
            //sh 'docker container rm ${app_name}'
            //sh 'docker image prune -f'

            archiveArtifacts(artifacts: '''
                    **/target/**/*.war, 
                    **/target/**/*.jar, 
                    **/target/jtest/sa/**, 
                    **/target/jtest/ut/**, 
                    **/target/jtest/monitor/**, 
                    **/soatest/report/**''',
                fingerprint: true, 
                onlyIfSuccessful: true,
                excludes: '''
                    **/.jtest/**, 
                    **/metadata.json'''
            )

            deleteDir()
        }
    }
}