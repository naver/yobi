/**
 * MyProjectListCtrl
 */
var MyProjectListCtrl = function($scope, $filter) {
  $scope.aProject = aProject;
  $scope.aCreatedProject = $filter('filter')($scope.aProject, {owner: sLoginId});
  $scope.aBelongedProject = _.difference($scope.aProject, $scope.aCreatedProject);
  $scope.sPredicate = "";
  $scope.bReverse = false;

  /**
   * orderByDate
   */
  $scope.orderByDate = function() {
    $scope.sPredicate = "createdDate";
    $scope.bReverse = !$scope.bReverse;
  };  

  /**
   * orderByName
   */
  $scope.orderByName = function() {
    $scope.sPredicate = "name";
    $scope.bReverse = !$scope.bReverse;
  };
};
